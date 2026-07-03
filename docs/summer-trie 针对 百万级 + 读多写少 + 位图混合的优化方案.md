summer-trie 针对"百万级 + 读多写少 + 位图混合"的优化方案
本方案面向十万到百万级数据、写入不频繁、可定期重建的场景，分三档（纯优化 → 混合位图 → 深度改造）给出可落地的改动清单、用到的扩展点、预期收益，以便按阶段推进。

背景与现状瓶颈
通读 @d:\Develop\Personal\category_project\summer-trie\src\main\java\top\chitucao\summerframework\trie\MapTrie.java、BasicOperates、DefaultNodeManager、NodeFactoryRegistry 后，在百万级数据 × 多条件查询的场景下，CPU 热点主要有 4 类：

TreeMap 范围查询的无谓拷贝（@d:\Develop\Personal\category_project\summer-trie\src\main\java\top\chitucao\summerframework\trie\operation\BasicOperates.java:254）：NavigableMap.tailMap/headMap/subMap 本是 O(1) view，但被 NodeFactoryRegistry.newChildren(result) 的 new TreeMap<>(childMap) 强行落地成 O(m log m) 拷贝。
HashMap 分支所有 O(n) 操作都产生一个新 Map（BasicOperates:40-212）：每层 searchAndAgg 都分配一个中间 HashMap，百万数据 × 多层过滤 × QPS 下 GC 压力明显。
Stream.flatMap 在 levelSearch/fastErase 里的 lambda 装箱（MapTrie.java:333-337 / 187-199）：每层一次 stream 构造。
IN/NIN 每次 new HashSet（BasicOperates:164, 198, 326, 360）：即使 list 只有 3 个值也分配。
在百万级数据、选择性低的字段（价格、日期）走 Trie 范围 + 选择性高的字段（enum / id）走位图，是业内经验上最优的结构。

阶段一：纯内部优化（兼容，不改 API）
目标：不动任何公共 API，把上面 4 个热点处理掉，预期查询 CPU 下降 30%~60%、GC 分配量下降 80%+。

1.1 TreeMap 范围操作直接复用 subMap view
位置：@d:\Develop\Personal\category_project\summer-trie\src\main\java\top\chitucao\summerframework\trie\operation\BasicOperates.java:249-317
改动：NAVIGABLE_MAP_GT_OP / GTE / LT / LTE / BETWEEN 直接 return (Map) ((NavigableMap) childMap).subMap(...)，去掉 NodeFactoryRegistry.getInstance()...newChildren(result) 这层。
注意：下游不能对返回结果做写操作（目前所有调用点都是只读遍历，安全）。可以加个 unmodifiableNavigableMap 保护。
1.2 HashMap 分支改"懒视图"返回
位置：BasicOperates.HASH_MAP_*_OP（NE/GT/GTE/LT/LTE/BETWEEN/IN/NIN 8 个）。
改动方式 A（推荐）：抽象一个 FilteredChildMap 轻量类，只持有 childMap 引用 + Predicate<Entry>，实现 entrySet()/size()/isEmpty()。下游的 Stream.flatMap(e -> map.values().stream()) 直接消费，不需要再物化。
改动方式 B（保守）：把返回类型仍为 Map，但传入一个可复用的 thread-local HashMap（ThreadLocal<HashMap> 池化 + clear() 复用），避免每次 new。
1.3 levelSearch / dfsSearch 把 Stream 换成迭代
位置：@d:\Develop\Personal\category_project\summer-trie\src\main\java\top\chitucao\summerframework\trie\MapTrie.java:329-362 和 490-547。
改动：用显式的 ArrayDeque<Node> / for-each 替换 Stream<Node>.flatMap(...)。JIT 对短生命周期 stream 还不错，但 lambda capture 仍会分配 Supplier/Function；改成纯 loop 后热路径基本零分配。
1.4 IN/NIN 短路优化 + 字典 key 化
位置：BasicOperates.HASH_MAP_IN_OP / NAVIGABLE_MAP_IN_OP。
改动：
条件构造时就把 List<R> 通过 property.mappingNodeKey 转成 primitive key 数组（int[]/long[]）存进 Criterion，查询时免掉 new HashSet<>(list)。
keys.size() <= 8 时走线性数组扫描，避免 HashSet 开销（字典 id 化后差距更明显）。
1.5 字典基数自动优先级（零成本 CBO）
位置：MapTrie 构造器 + Configuration。
改动：新增 Configuration.setAutoReorderByCardinality(true)。构造 nodeManagers 链表时根据当前 Dict.size() 升序排。这在"启动加载完数据才第一次查"的场景里天然生效（百万数据全量灌完再查询，正好）。
注意：开关默认关闭，避免破坏用户显式设计的字段顺序语义。
1.6 Criteria Primitive 化
位置：@d:\Develop\Personal\category_project\summer-trie\src\main\java\top\chitucao\summerframework\trie\query\Criteria.java。
改动：Criterion 从 LinkedHashMap<String, Object> 改成紧凑数组（int[] opCodes + Object[] values）。百万次构造 Criteria 时减少 LinkedHashMap 桶分配。优先级中低，如果不是 QPS 瓶颈可以不改。
预期收益汇总：

TreeMap 范围查询：百万数据量下单次 100ms 级降到 10ms 级（命中率 20% 时）
中间对象分配量：从 O(深度 × 命中数) 降到 O(深度)
总体 CPU：读多写少场景下下降 30%~60%
阶段二：Trie + Roaring Bitmap 混合方案（核心）
目标：把位图挪到叶子层做多条件 AND，利用 summer-trie 已有的扩展点（自定义 NodeType + NodeFactory + Operate）无侵入接入。

2.1 架构
Level 0 ── 日期          TreeMapNode         // 范围字段走 TREE_MAP
Level 1 ── 价格          TreeMapNode         // 范围字段走 TREE_MAP
Level 2 ── 出发城市 id    HashMapNode         // 中等基数走 HASH_MAP
Level 3 ── 抵达城市 id    HashMapNode
Level N ── 叶子          BitmapNode(新增)    // 所有低选择性字段折叠到这里
                          └─ 内部按"字段→Roaring Bitmap<docId>"组织
也可以把叶子层改成两层：

Level N-1 ── docId 集合    BitmapNode
Level N   ── 原始数据(可选) AutoMappingProperty(Function.identity)
2.2 核心扩展点清单
扩展	文件	主要实现
新 NodeType BITMAP	NodeType.java	添加常量
BitmapNode<K>	新建 node/BitmapNode.java	内部用 Map<字段id, Map<字段值 id, RoaringBitmap>>
BitmapNodeFactory	新建，注册到 NodeFactoryRegistry	newNode / newChildren
BITMAP_EQ/IN/NIN/NE/AND_OP 等	新建 BitmapOperates，注册到 OperationRegistry	RoaringBitmap.and/or/andNot
BitmapLeafProperty	新建，继承 AbstractProperty	insert 时把"该条数据在哪些字段取什么值"展开成 bitmap
2.3 接入依赖
<dependency>
    <groupId>org.roaringbitmap</groupId>
    <artifactId>RoaringBitmap</artifactId>
    <version>1.3.0</version>
    <scope>compile</scope>
</dependency>
Roaring Bitmap 对稀疏/稠密数据都有压缩，百万文档的位图通常几百 KB 到几 MB，远小于原始数据。

2.4 查询路径
Criteria.where("日期").between(d1, d2)        // 走 TreeMap 子视图，O(log n + m)
        .and("价格").between(p1, p2)           // 走 TreeMap 子视图
        .and("车次类型").in("GD","D","K")       // 走 HashMap
        .and("坐席类型").eq("一等座")            // ← 最后一层 BitmapNode.eq("seatClass", "一等座")
        .and("车次类型").in("GD","D","K")       // ← 继续在 Bitmap 层 AND
设计要点：

前半程 Trie 做"按前缀裁剪"，把候选 docId 范围压缩到 数万级。
最后 BitmapNode 拿到子树里所有 docId 的 RoaringBitmap（可预先聚合），再和剩余字段的 bitmap 做 and，SIMD 级速度。
BitmapNode.getResultDocIds(criteria) 返回 RoaringBitmap，MapTrie.dataSearch 用这个 bitmap 从数据仓直接取 POJO。
2.5 构建 / 重建流程
既然你写入不频繁、可以定期重建：

1. 从数据源加载 List<T>
2. 按 docId 建 Map<Long, T> 数据仓（off-heap 更好）
3. 对每个字段建 RoaringBitmap<docId>
4. 遍历数据依次 insert 到 Trie，叶子层走 BitmapLeafProperty
5. 原 trie 热替换（AtomicReference<MapTrie> 双缓冲）
重建期间旧树继续服务，对业务零感知。

2.6 预期收益
查询 CPU：在位图能覆盖的字段组合上，相比纯 Trie 提速 3~10 倍（等值 AND 多字段最明显）。
内存：
字符串字段（车次、坐席）经 Roaring Bitmap 压缩后，比纯 Trie 节点数量大幅减少。
对百万数据、20 个字段的情况，整体内存通常降到纯 Trie 的 40%~60%。
保留 Trie 优势：日期/价格等范围查询仍然走 TreeMap 子视图，依然 O(log n + m)，正好补齐"位图对高选择性字段不友好"的短板。
阶段三：深度改造（可选，视收益决定）
这些改动有一定侵入性，建议只在阶段一二不够时再考虑：

3.1 Bit-Sliced Index（BSI）
把"价格"这种连续数值字段用 BSI 存储（log2(max) 个 bitmap），位图直接支持范围查询。和 Trie 的 TreeMap 两套方案二选一，数据量越大 BSI 越划算。

3.2 Query codegen
把 Criteria 编译成专用 Java 字节码（Janino/ASM），完全消除 lambda / 反射调用开销。Druid / Pinot 的高性能查询路径都这么做。

3.3 Off-heap 字典 / 数据仓
数据仓用 OHC / Chronicle-Map / memory-mapped file，GB 级数据避免 GC 大对象。

实施顺序建议
迭代	内容	风险	预期开发量
M1	阶段一 1.1 ~ 1.4	低（纯内部改）	1~2 天
M2	阶段一 1.5 ~ 1.6 + 基准 benchmark	低	1 天
M3	阶段二 BitmapNode + 基础操作	中	3~5 天
M4	阶段二 业务接入 + 双缓冲热替换	中	2~3 天
M5	阶段三（按需）	高	看实际场景
必要的验证
基准：使用 JMH 编写 TrieBenchmark，对比 before / after，覆盖：
纯等值 3 字段 AND（testCache 类似场景）
范围 + 等值混合（testMultiConditionSearch）
百万数据全量 listSearch
回归：复用现有 TrieTest，补充"配置 autoReorderByCardinality 后等价性"用例。
内存：用 JOL（jol-core）测量优化前后 trie 对象图大小。
需要你确认的事项
是否接受引入 RoaringBitmap 作为新 NodeType 的依赖（默认编译域，不用的项目可以排除）。
业务字段里哪些字段选择性高（基数大 / 过滤率低）适合走 Trie，哪些字段基数小 / 高频等值适合走 Bitmap。可以先扫一轮现有数据统计字典基数。
是否愿意在 M1/M2 做完后先上线观测一轮，再决定是否进入 M3。