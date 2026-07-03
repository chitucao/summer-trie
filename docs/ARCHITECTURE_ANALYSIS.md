# summer-trie 架构与实现分析

> 版本：`1.1.5.RELEASE` | JDK：`1.8` | 基于全量源码阅读产出

## 目录

1. 项目定位
2. 顶层架构
3. 核心组件详解
4. 关键设计思想
5. 核心流程源码走读
6. 复杂度与性能优化
7. 扩展点
8. 潜在问题与可改进点
9. 总结

## 一、项目定位

`summer-trie` 是一个**节点值可为任意数据类型的通用前缀树（Trie）组件**。与经典字符串 Trie 不同，这里每一层代表业务对象的一个字段，一条从根到叶的路径就对应一条完整的业务记录。

它本质上是一个**内存里的多维倒排 / 多字段前缀索引**，适合这类场景：

- 热点业务数据常驻内存（缓存型），访问多但低频更新。
- 需要在同一份数据上支持多种查询维度（出发日期 / 出发城市 / 价格 / 舱等 …）。
- 想在 `HashMap` 之外同时获得**范围查询 / 排序 / 区间聚合**能力。
- 字段值重复度高，希望做**字典压缩**以节省内存。

注释和测试里用航班 / 火车数据作为主要示例。`@d:\Develop\Personal\category_project\summer-trie\src\test\java\top\chitucao\summerframework\trie\TrieTest.java:934-1013` 展示了一段“用 Trie 替代多个 HashMap 缓存”的完整对比。

## 二、顶层架构

### 2.1 包结构

```text
top.chitucao.summerframework.trie
├── Trie                 // 对外接口
├── MapTrie              // Trie 的唯一实现（核心编排类）
├── codec                // ProtoBuf 生成类（序列化功能目前被注释掉，未启用）
├── configuration
│   ├── Configuration    // 属性集合 + 全局开关
│   └── property         // Property 体系（节点属性抽象 + 4 种实现）
├── dict                 // Dict 抽象 + HashMapDict 实现
├── node
│   ├── Node / AbstractMapNode
│   ├── HashMapNode / TreeMapNode / ConcurrentHashMapNode / ConcurrentSkipListMapNode
│   ├── NodeType / NodeFactory / NodeFactoryRegistry
│   └── extra/ AbstractNamedMapNode + NamedHashMapNode + NamedTreeMapNode
├── nodemanager
│   ├── NodeManager / DefaultNodeManager
│   └── NodeManagerFactory / DefaultNodeManagerFactory
├── operation
│   ├── Operation / Operate / Func
│   ├── BasicOperates        // 预置 18 个操作实现
│   └── OperationRegistry
├── query
│   ├── Criteria / Criterion / Aggregation / Aggregations
│   ├── ResultBuilder
│   └── TreeNode
└── utils/Pair
```

### 2.2 组件关系（一图流）

```text
┌──────────────────── MapTrie ────────────────────┐
│  root : Node              ← 实际的树根          │
│  nodeManagers : LinkedList<NodeManager>         │
│  nodeManagerNameMap : Map<String, NodeManager>  │
└──────┬─────────────────────────────────┬────────┘
       │                                 │
       ▼                                 ▼
   NodeManager(层 0) ── next ──► NodeManager(层 1) ── next ──► … ── next ──► NodeManager(层 N-1)
     │                              │                                   │
     ▼                              ▼                                   ▼
   Property                      Property                            Property
     │ (isDictProperty?)            │                                   │
     ▼                              ▼                                   ▼
    Dict (可选)                    Dict                                Dict

Node(每一层) ── 由 NodeFactory 按 Property.nodeType() 创建
              ── children 的 key = Property.mappingNodeKey() 产生的压缩键
              ── children 的 value = 子 Node
查询 ── OperationRegistry.getOperate(nodeType, op).query(...) ── 返回过滤后的 childMap
```

核心分层关系可以概括成一个等式：

```text
MapTrie = 树结构(Node) × 每层控制器(NodeManager) × 字段定义(Property) × 字典(Dict) × 操作策略(Operate)
```

## 三、核心组件详解

### 3.1 Trie / MapTrie

`@d:\Develop\Personal\category_project\summer-trie\src\main\java\top\chitucao\summerframework\trie\Trie.java:17-141` 定义了对外 API，可以分成四组：

| 分组 | API | 说明 |
|---|---|---|
| 元数据 | `getRoot` / `getDepth` / `getSize` | 根节点、层数、数据条数 |
| 写 | `insert(T)` / `erase(T)` / `erase(Criteria)` | 插入、按数据删、按条件删 |
| 存在性 | `contains(T)` / `contains(Criteria)` | 判存在 |
| 查 | `dataSearch` / `propertySearch` / `listSearch` / `treeSearch` / `dictValues` / `queryAsTreeNode` | 五种查询形态 |

`@d:\Develop\Personal\category_project\summer-trie\src\main\java\top\chitucao\summerframework\trie\MapTrie.java:23-52` 是唯一实现，构造器做了三件事：

1. `checkAndResolveConfiguration` 校验属性非空，并自动判断**叶子节点是否可直接反查原对象**（`leafNodeAsDataNode`，下面专门讲）。
2. 用 `DefaultNodeManagerFactory` 把每个 `Property` 包成 `NodeManager`，按层级排序后串成双向链表。
3. 根据头节点 `property.nodeType()` 创建 `root`。

> **关键设计**：`MapTrie` 本身不直接操作 `Node`，而是把“这一层能干什么”都委托给对应的 `NodeManager`。`MapTrie` 里几乎所有算法都是 **“迭代 `nodeManagers` + 通过当前 `NodeManager` 的方法处理当前层”** 的模式，非常干净。

### 3.2 Configuration + Property 体系

`@d:\Develop\Personal\category_project\summer-trie\src\main\java\top\chitucao\summerframework\trie\configuration\Configuration.java:12-86`：

- `properties`：`LinkedHashMap<String, Property>`，按插入顺序决定层级。
- `addProperty(property)` 自动分配 `level`。
- 两个全局开关：
  - `useFastErase`：是否启用快速删除（默认 true）。
  - `leafNodeAsDataNode`：叶子节点是否存着原对象（由 `MapTrie` 自动推断，见 4.3）。

**Property** 的类图（只有一层继承链）：

```text
Property (interface)
  └── AbstractProperty  (name / level / nodeType / isDictProperty / object2FieldMapper)
        ├── OriginProperty            isDictProperty=false   节点 key = 字段值本身
        ├── MappingProperty           isDictProperty=false   双向自定义映射
        └── AbstractDictProperty      isDictProperty=true    内置 HashMapDict
              ├── OneWayMappingProperty   只需要 field → nodeKey 的映射
              └── AutoMappingProperty     自动自增发号（BYTE/SHORT/INT/LONG）
```

四种 Property 的行为差异体现在 `mappingNodeKey` / `mappingOrCreateNodeKey` / `nodeKey2FieldValue` 三个方法上。差别归结起来是两个维度：

| 维度 | 描述 |
|---|---|
| **是否带字典** | `isDictProperty()` 决定“节点 key”和“字段值”之间是否通过 `Dict` 做映射。带字典 = 能做空间压缩。 |
| **如何决定节点 key** | `Origin` = 原样；`Mapping` = 用户自定义双向函数；`OneWay` = 只写 forward，反查靠字典；`Auto` = 字典自己发 id。 |

最典型的组合是 `AutoMappingProperty` + 字符串字段：原始字符串只在 `Dict` 里存一份，前缀树里只放一个 `byte/short/int` 作为 key，内存占用大幅降低。

### 3.3 Dict

`@d:\Develop\Personal\category_project\summer-trie\src\main\java\top\chitucao\summerframework\trie\dict\Dict.java:13-82` 定义了字典协议，`HashMapDict` 是唯一实现，内部维护了三个 `ConcurrentHashMap`（见 `@d:\Develop\Personal\category_project\summer-trie\src\main\java\top\chitucao\summerframework\trie\dict\HashMapDict.java:16-30`）：

- `dict: K → R`：nodeKey → 字段值。
- `indexer: R → K`：字段值 → nodeKey（反向索引，用于判重 / 查询）。
- `counter: K → Integer`：nodeKey 被引用的次数（给 erase 使用）。

**设计亮点**：字典和 Trie 结构**完全解耦**。同一个 `Property` 对象可以被多棵 Trie 共享，于是多个索引树可以**复用同一份字典数据**（见 `testDictReUse` 用例）。

### 3.4 Node

`@d:\Develop\Personal\category_project\summer-trie\src\main\java\top\chitucao\summerframework\trie\node\Node.java:13-73` 抽象了“能产生 `children` Map 的节点”。具体实现是一个 2×3 的矩阵：

|  | 哈希 | 有序 | 有序 + 并发 |
|---|---|---|---|
| 普通 | `HashMapNode` | `TreeMapNode` | `ConcurrentSkipListMapNode` |
| 线程安全哈希 | `ConcurrentHashMapNode` | — | — |
| 带 `name` 字段（便于序列化展示） | `NamedHashMapNode` | `NamedTreeMapNode` | — |

**设计目的**：同一棵树的不同层级可以选不同的节点类型。例如：

- 枚举型字段（车次、舱等）→ `HASH_MAP`，O(1) 查找。
- 数值型范围字段（价格、日期）→ `TREE_MAP`，支持 `NavigableMap.subMap/headMap/tailMap`，范围查询从 O(n) 降到 O(log n + m)。
- 多线程并发写入场景 → `CONCURRENT_HASH_MAP` / `CONCURRENT_SKIP_LIST_MAP`。

`@d:\Develop\Personal\category_project\summer-trie\src\main\java\top\chitucao\summerframework\trie\node\NodeFactoryRegistry.java:17-329` 用**注册表 + 工厂**模式管理这些节点类型，扩展只需调用 `registerNodeFactory(...)`。

### 3.5 NodeManager

`@d:\Develop\Personal\category_project\summer-trie\src\main\java\top\chitucao\summerframework\trie\nodemanager\NodeManager.java:19-142` 是这套架构里的“大总管”，每一层一个实例。它的 API 可以分成三类：

1. **节点生命周期**：`createNewNode` / `addChildNode` / `removeChild` / `removeChildNode` / `findChildNode`。
2. **字典 ↔ 节点 key 互转**：`mappingDictKey` / `mappingDictValues`。
3. **当前层的查询**：`search` / `searchAndAgg` / `contains` / `slice` / `remove`。

在 `@d:\Develop\Personal\category_project\summer-trie\src\main\java\top\chitucao\summerframework\trie\nodemanager\DefaultNodeManager.java:70-84` 的 `addChildNode` 里能清晰看到写入时的三步动作：

```java
K dictKey = property1.mappingOrCreateNodeKey(val);
if (property1.isDictProperty()) {
    property1.getDict().put(dictKey, val);
}
return parent.putChild(dictKey, childSupplier);
```

`NodeManager` 之间用 `prev` / `next` 形成双向链表（见 `@d:\Develop\Personal\category_project\summer-trie\src\main\java\top\chitucao\summerframework\trie\nodemanager\DefaultNodeManagerFactory.java:31-39`），让 `MapTrie` 的算法既能前向遍历（`insert` / `dataSearch`），也能从叶子往上回溯（`erase` 时清理空父节点）。

### 3.6 Operation / Operate / Func

定义在 `@d:\Develop\Personal\category_project\summer-trie\src\main\java\top\chitucao\summerframework\trie\operation\Operation.java:15-52`：

```text
EQ / NE / GT / GTE / LT / LTE / BETWEEN / IN / NIN / FUNC
```

每种操作在两类节点上各有一套实现，位于 `BasicOperates`：

- `HASH_MAP_*_OP`：针对哈希节点，`EQ`/`IN` 单值是 O(1)，其它靠遍历 O(n)。
- `NAVIGABLE_MAP_*_OP`：针对 `TreeMap` / `ConcurrentSkipListMap`，范围类操作利用 `NavigableMap.subMap/headMap/tailMap` 直接切片，成本 O(log n + m)。

`OperationRegistry` 用 **“节点类型 × 操作名”** 二维 Map 做派发（`@d:\Develop\Personal\category_project\summer-trie\src\main\java\top\chitucao\summerframework\trie\operation\OperationRegistry.java:22-108`），支持用户通过 `registerOperation(nodeType, name, operate)` 注册自定义算子（测试里演示了 `$$customOp`，约定前缀防命名冲突）。

`Func` 是一次性的自定义函数，作为 `Criteria.func(...)` 的语法糖，不需要全局注册。

### 3.7 Criteria / Criterion / Aggregation / ResultBuilder

- `Criteria` 是 **fluent builder**：`Criteria.where("depCityId").between(0,1005).and("trainType").eq("KS")`；每个 `and(key)` 创建一个新节点挂到 `criteriaChain`，`getAllCriterion()` 时把同字段的条件合并成一个 `Criterion`。
- `Criterion` 本身就是 `LinkedHashMap<操作名, 值>`（同字段多条件用同一个 map 顺序执行）。
- `Aggregations` 目前只支持 `MIN / MAX`。
- `ResultBuilder` 用 `BiConsumer<E, R>` 做 setter 注入，避免 `listSearch` 依赖反射。

## 四、关键设计思想

### 4.1 字典与索引分离 → 压缩 + 复用

`AutoMappingProperty("trainType", DictKeyType.BYTE)` 把一个 `String trainType` 在 trie 中的存储从 16+ 字节压到 1 字节，而 `trainType` 原值只在 `HashMapDict` 里出现一份。

得益于字典是 `Property` 的内部状态，**同一个 `Property` 实例可以被多棵 trie 复用**，节省重复字典空间。这是 `@d:\Develop\Personal\category_project\summer-trie\src\test\java\top\chitucao\summerframework\trie\TrieTest.java:1017-1065` 的 `testDictReUse` 的核心诉求（但该用例本身有个 bug，详见第 8 节）。

### 4.2 “每层独立选型” 的设计自由度

不同业务字段可以独立选择 Property 类型 × Node 类型 × 操作集合：

| 字段特征 | 推荐组合 |
|---|---|
| 字符串枚举，频繁等值查 | `AutoMappingProperty` + `HASH_MAP` |
| 数值/日期，需要范围 | `OneWayMappingProperty` + `TREE_MAP` |
| 高基数字符串，仅存在性判断 | `OneWayMappingProperty` + `HASH_MAP` |
| 多线程并发写 | `CONCURRENT_HASH_MAP` / `CONCURRENT_SKIP_LIST_MAP` |
| 需要 JSON 导出可读性 | `NAMED_HASH_MAP` / `NAMED_TREE_MAP` |

### 4.3 叶子节点做数据节点：`leafNodeAsDataNode`

`@d:\Develop\Personal\category_project\summer-trie\src\main\java\top\chitucao\summerframework\trie\MapTrie.java:962-977` 用一个巧妙的小技巧自动判断最后一层是否存着原对象：

```java
Object obj = new Object();
return obj == property.mappingFieldValue(obj);
```

如果 `object2FieldMapper` 是 `Function.identity()`，则传入对象原地返回，判定为“叶子即数据节点”。此时 `dataSearch` 可以直接从最后一层字典返回 `T` 本身；反之则禁止调用 `dataSearch`。这个设计让 `Trie` 在两种模式之间无缝切换：

- 叶子存数据（`dataProperty.setObject2FieldMapper(Function.identity())`）：可做 `dataSearch`、按 id 反查数据。
- 叶子不存数据（比如只留一个 `id`）：走 `listSearch + ResultBuilder` 重新拼装业务对象。

### 4.4 策略的三重扩展：工厂 + 注册表 + 双向链表

| 维度 | 扩展入口 | 复用方式 |
|---|---|---|
| 节点类型 | `NodeFactoryRegistry.registerNodeFactory(nodeType, factory)` | 新增类型时无需改核心代码 |
| 操作类型 | `OperationRegistry.registerOperation(nodeType, name, op)` | 可以针对新节点批量注册，或为已有节点加自定义算子 |
| 层级控制 | `DefaultNodeManagerFactory` → 自定义 `NodeManagerFactory` | 需要多层协作的语义可以在这里拦截 |

## 五、核心流程源码走读

### 5.1 `insert(T)`

`@d:\Develop\Personal\category_project\summer-trie\src\main\java\top\chitucao\summerframework\trie\MapTrie.java:92-97`：

```java
public void insert(T t) {
    Node<?> cur = root;
    for (NodeManager nodeManager : nodeManagers) {
        cur = nodeManager.addChildNode(cur, t);
    }
}
```

每一层：

1. `Property.mappingFieldValue(t)` 取字段值。
2. `Property.mappingOrCreateNodeKey(val)`：字典属性时若已存在返回老 key，否则让字典自增发号。
3. 字典属性额外 `dict.put(dictKey, val)` 更新三份 map 与引用计数。
4. `parent.putChild(dictKey, childSupplier)`：若子节点已存在直接返回，否则由下一层的 `NodeFactory` 创建（注意 `childSupplier = next().createNewNode()`，用下一层的节点类型来建下一层节点）。

**注意**：重复 `insert` 同一条数据会在每一层命中已存在的子节点，但**字典 counter 会被重复递增**（`HashMapDict.put` 里 `counter.put(k, counter.getOrDefault(k,0)+1)`）。即 `insert` 不是幂等的，后续用 `normErase` 时 counter 会偏高 —— 这是 `useFastErase=true` 模式下可以接受的代价，但在非快速模式下要注意。

### 5.2 `erase(T)`

`@d:\Develop\Personal\category_project\summer-trie\src\main\java\top\chitucao\summerframework\trie\MapTrie.java:207-252` 走三趟：

1. **前向查找**（边走边把父节点压进 `parentNodeStack`），找不到直接返回。
2. **从叶子回溯**：删除子节点，如果父节点变空就继续往上删。
3. **减字典计数**：每一层字典属性 `decrNodeKeyCount`，最后一层 `removeNodeKey`。

这里充分利用了 `NodeManager.prev()` 的反向链表能力，保证空父节点不会被遗留。

### 5.3 `erase(Criteria)`：`fastErase` vs `normErase`

由 `configuration.isUseFastErase()` 决定路径：

#### `fastErase` → `@d:\Develop\Personal\category_project\summer-trie\src\main\java\top\chitucao\summerframework\trie\MapTrie.java:187-199`

```java
for (int i = 0; i < maxCriteriaLevel; i++) {
    NodeManager nodeManager = iterator.next();
    cur = cur.flatMap(e -> nodeManager.search(e, criterion).values().stream());
}
NodeManager lastNodemanager = iterator.next();
cur.forEach(e -> lastNodemanager.remove(e, criterion));
```

只下降到“有过滤条件的最深那一层”，直接 `remove` 子树。**不更新字典 counter、不递归清理空父节点、不返回准确数量**（对外返回 -1）。优点是极快，适合“整棵 trie 定时重建”的缓存场景。

#### `normErase` → `@d:\Develop\Personal\category_project\summer-trie\src\main\java\top\chitucao\summerframework\trie\MapTrie.java:123-179`

递归自顶向下走到叶子，返回删除数量；特别地用**返回负数** `-size` 标记“我这棵子树已经空了，父节点请把我摘掉”。优点：

- 返回真实删除数。
- 正确维护 `counter` 和 `dict`（可选配合 `HashMapDict.removeDictIfNonCount=true` 清理字典）。
- 清理空父节点，保持树紧凑。

### 5.4 统一的查询骨架：`levelSearch` + `multiLevelSearch`

单层查询 `dataSearch` / `propertySearch` 都落到 `@d:\Develop\Personal\category_project\summer-trie\src\main\java\top\chitucao\summerframework\trie\MapTrie.java:329-362` 的 `levelSearch`：

1. **前进阶段**：从 root 开始，流式 `flatMap` 到“展示层”之前，用每层的 `criterion` 收敛子树。
2. **展示层**：对当前节点做 `searchAndAgg`，用 `.keySet()` 收集该层的字典 key。
3. **兜底剪枝**：如果展示层之后还有 `criterion`，就对候选 key 逐个走 `childrenAnyMatch` 看下游是否至少有一条路径满足。

多层查询 `listSearch` / `treeSearch` / `queryAsTreeNode` 落到 `@d:\Develop\Personal\category_project\summer-trie\src\main\java\top\chitucao\summerframework\trie\MapTrie.java:490-547` 的 `multiLevelSearch` → `dfsSearch`，把“所有满足条件的路径”展平成 `List<List<?>>`，再由上层按需装配：

- `listSearch`：配合 `ResultBuilder` 组装成 `List<E>`。
- `treeSearch`：第一层是 `HashMap`，最后一层是 `ArrayList`，中间层全是 `HashMap`。
- `queryAsTreeNode`：组装成 `TreeNode`（便于 Jackson 序列化为前端可渲染的树）。

### 5.5 查询派发：`searchAndAgg`

`@d:\Develop\Personal\category_project\summer-trie\src\main\java\top\chitucao\summerframework\trie\nodemanager\DefaultNodeManager.java:124-184`：

1. 先 `search(cur, criterion)` 过滤子节点（走 `OperationRegistry`）。
2. 如果有 `Aggregation`，判定结果 map 是否 `NavigableMap`：
   - 是：`firstEntry/lastEntry`，O(log n)。
   - 否：遍历求 min/max，O(n)。

> 这里是整个框架性能的关键开关：**把字段放到 `TREE_MAP` 节点上，`MIN/MAX` 就能 O(log n)**。

## 六、复杂度与性能优化

### 6.1 单层操作复杂度矩阵

| 操作 | `HASH_MAP` 节点 | `TREE_MAP` / `SKIP_LIST` 节点 |
|---|---|---|
| `EQ` 单值 | O(1) | O(log n) |
| `IN` 单值 | O(1) | O(log n) |
| `IN` 多值 | O(n)（遍历 childMap） | O(n) |
| `NE` / `NIN` | O(n) | O(n) |
| `GT` / `GTE` / `LT` / `LTE` | O(n) | **O(log n + m)**（`NavigableMap.tailMap/headMap`） |
| `BETWEEN` | O(n) | **O(log n + m)**（`subMap`） |
| `MIN` / `MAX`（聚合） | O(n) | O(log n) |

对应源码分别在 `BasicOperates.HASH_MAP_*` 和 `BasicOperates.NAVIGABLE_MAP_*`，`DefaultNodeManager.searchAndAgg` 选分支。

### 6.2 空间压缩收益（以车次数据为例）

`buildConfiguration4` 里（`@d:\Develop\Personal\category_project\summer-trie\src\test\java\top\chitucao\summerframework\trie\TrieTest.java:1178-1235`）：

- `trainType`（字符串，约 2~4 字节 + 对象开销 → 用 `AutoMappingProperty + DictKeyType.BYTE`）：每次存储 1 字节。
- `departureStationCode`（3 字母代码 + 对象开销 → `DictKeyType.INT`）：每次存储 4 字节。
- `departureCityId / arrivalCityId`（`int` → `OriginProperty`）：无字典开销，直接存原值。

因为 Trie 自身会把“前缀相同”的数据共用一段路径，进一步节省空间；对于 3000 条车次数据，序列化后 dump 文件（`@d:\Develop\Personal\category_project\summer-trie\src\test\resources\train_resource_dump.dat` ~389KB） 明显小于原 JSON（~1.5MB）。

### 6.3 多索引复用字典

同一个 `Property` 实例给两棵 trie 用（示例见 4.1），字典只存一份。注意事项：

- **必须复用的是 `Property` 实例**，因为 `Dict` 是 `AbstractDictProperty` 的成员。
- 如果 trie1 / trie2 的字段顺序不同，它们仅共享 `Dict`，`Node` 结构还是各自独立的。

### 6.4 `useFastErase` 的权衡

| 维度 | `fastErase` | `normErase` |
|---|---|---|
| 速度 | ⭐⭐⭐⭐⭐ | ⭐⭐ |
| 返回删除数 | `-1` | 真实数量 |
| 维护 `dict.counter` | 否 | 是 |
| 清理空父节点 | 否 | 是 |
| 适用 | 定期重建的缓存 | 常态增删的索引 |

## 七、扩展点

按使用频率从高到低：

1. **注册新的操作算子**：`OperationRegistry.registerOperation(NodeType.HASH_MAP, "$$myOp", op)`，约定 `$$` 前缀防与内置冲突。测试里 `testCustomOp` 有完整示例。
2. **使用临时自定义函数**：`Criteria.func((childMap, property) -> ...)`；一次性逻辑，不需要全局注册。
3. **新增 Property 类型**：继承 `AbstractProperty` / `AbstractDictProperty`，实现 3 个核心映射方法即可；注意正确设置 `isDictProperty` 与 `nodeType`。
4. **替换 Dict 实现**：`AbstractDictProperty` 的三参数构造器支持传入自定义 `Dict`（比如换成堆外存储、Caffeine 等）。
5. **新增 NodeType**：
   - 先 `registerNodeFactory(new MyNodeType, new MyNodeFactory)`。
   - 再 `registerOperation(MyNodeType, op, ...)` 注册该类型的全部算子（至少要覆盖项目里会用到的）。
6. **替换 NodeManager**：实现 `NodeManagerFactory` 并在 MapTrie 构造器里传入（目前 `MapTrie` 写死了 `DefaultNodeManagerFactory`，如需替换要改一行源码或改为可注入——见第 8 节建议）。

## 八、潜在问题与可改进点

### 8.1 已知 Bug

- **`testDictReUse` 中 `trie2` 用错配置**（`@d:\Develop\Personal\category_project\summer-trie\src\test\java\top\chitucao\summerframework\trie\TrieTest.java:1055`）：

  ```java
  Configuration configuration2 = new Configuration();
  configuration2.addProperty(depCityCodeProperty);
  configuration2.addProperty(depDateProperty);
  Trie<FlightResourceDO> trie2 = new MapTrie<>(configuration1);   // ← 这里应是 configuration2
  ```

  用例虽然能通过，但失去了“两棵 trie 不同字段顺序 / 复用字典”的验证价值。

- **`AbstractProperty.setLevel(level)` 在共享 Property 的场景下会冲突**：`Configuration.addProperty` 会覆盖 `level`，于是复用同一个 `Property` 实例到两棵 trie 上时，后加入的会把 `level` 改写成自己的层次，另一棵树的 `level` 就失效了。`@d:\Develop\Personal\category_project\summer-trie\src\main\java\top\chitucao\summerframework\trie\configuration\property\AbstractProperty.java:43-46` 把 `level` 存在 Property 上而不是 `NodeManager` 上，破坏了字典复用的前提。建议把 `level` 迁移到 `NodeManager`（它本来就是一层一个实例）。

### 8.2 并发安全的不一致

- `HashMapDict` 用 `ConcurrentHashMap`，且有 `ConcurrentHashMapNode` / `ConcurrentSkipListMapNode`，看似支持并发写。
- 但 `HashMapDict.put(k,v)` 的 `counter.put(k, counter.getOrDefault(k, 0) + 1)` **不是原子操作**，并发写入同一字典 key 会丢计数。应改为 `counter.merge(k, 1, Integer::sum)` 或用 `LongAdder`。
- `decrNodeKeyCount` 同样不是原子递减，且"减到 0 时自动 remove"在并发下可能造成 key 丢失后别的线程又把它 put 回去。
- `AutoMappingProperty.mappingOrCreateNodeKey` 里 `containsFieldValue + dictKeyAdder.nextKey()` 的 check-then-act 也不是原子的，同一字段值高并发 put 可能拿到不同 nodeKey（字典出现重复 fieldValue）。
- `Configuration` 的 `properties` 是普通 `LinkedHashMap`，多线程 `addProperty` 会坏。

**结论**：目前的并发安全只到“读并发友好”这一档。真想做并发写，字典侧至少需要原子 get-or-create。

### 8.3 `insert` 非幂等

重复 `insert` 同一条数据，counter 会被累加（见 5.1 末尾分析）。`normErase` 依赖 counter 做字典清理，这种情况下会导致 `decrNodeKeyCount` 后 counter 仍 > 0，字典永远不会被 GC（开启 `removeDictIfNonCount` 时）。
缓解方法：`HashMapDict.put` 时检测“已存在 nodeKey 且 fieldValue 相同则不递增 counter”。

### 8.4 `DefaultNodeManager.addChildNode` 对 null 值的处理

`@d:\Develop\Personal\category_project\summer-trie\src\main\java\top\chitucao\summerframework\trie\nodemanager\DefaultNodeManager.java:74-76`：

```java
if (val == null) {
    throw new IllegalArgumentException("Cannot add child node with null value. Property: " + property1.name() + "Data: " + t.toString());
}
```

- 异常消息 `property1.name() + "Data: "` 中间缺空格，拼接会是 `xxxData: yyy`。
- 更主要的问题是**整个 trie 不支持 null 字段**，业务上必须预先填默认值。文档里没有强调这一点。

### 8.5 `Configuration.setLeafNodeAsDataNode` 判定方式可能误伤

`checkNodeAsDataNode` 用 `new Object()` 调用 `mappingFieldValue`，如果映射函数里做了类型转换（例如 `((TrainSourceDO) o).getId()`）会抛 `ClassCastException`，然后 `catch` 掉返回 false。用户肉眼看代码很难预料自己的 mapper 是否走了这个 catch。

建议：让用户显式声明，或改用反射检测 mapper 是否 `==Function.identity()`。

### 8.6 API 一致性

- `propertySearch` 不支持 `Aggregations`（只有 `listSearch` / `treeSearch` 支持），但底层 `levelSearch` 实际是同一套，完全可以扩展。
- `erase(Criteria)` 在 fast 模式下返回 `-1`，但接口 JavaDoc 说“删除的数据条数”；调用方如果没看代码，会把 `-1` 当异常值。建议要么统一返回真实数量，要么改为 `boolean` 语义。

### 8.7 序列化被注释

`MapTrie` 里 `serialize` / `deserialize` / `dictSizes` 整块被注释掉（`@d:\Develop\Personal\category_project\summer-trie\src\main\java\top\chitucao\summerframework\trie\MapTrie.java:618-823`），但 `codec/MapTrieProtoBuf.java` 依然存在。`Trie` 接口注释了对应方法，测试里也都注掉了。建议要么删除 `codec` 包减包大小，要么补齐代码恢复功能（当前序列化后 ID 自增器恢复、字典类型映射都还不完善）。

### 8.8 小的代码质量问题

- `RESOUCE_FOLDER` 拼写错误（`RESOURCE`）。
- `MapTrie` 里硬编码 `DefaultNodeManagerFactory`，缺少把它设为可注入参数的构造器，阻碍了 7.6 的扩展。
- `Configuration.sortProperties` 是 public 的，但调用它后 `MapTrie` 已经构造完毕的话，`level` 被改写但 `nodeManagers` 链表顺序不会同步更新，属于“调用时机约束”的坑。
- `HashMapDict.removeDictIfNonCount` 没有 setter，用户无法切换开关。
- `BasicOperates` 的 HashMap 分支里，`GT/GTE/LT/LTE` 强转 `Comparable` 时只在第一个元素做校验，如果后面字典 key 非 `Comparable` 会抛 `ClassCastException`，缺少友好错误。

## 九、总结

### 9.1 整体评价

- **设计抽象清晰**：`Trie ↔ MapTrie` 薄封装 + `NodeManager` 链表承载每层语义 + `Property × Node × Operation` 三层正交扩展，是一套能在内存里做通用多维索引的可靠骨架。
- **工程落地感强**：字典压缩、字段复用、查询/删除快慢模式、多种 Node 类型适配不同场景，明显是从真实业务（典型的旅游/OTA 报价查询）抽象出来的。
- **可扩展性很好**：注册表 + 工厂的双重开放让新节点类型/新算子/新数据源能以低侵入方式接入。

### 9.2 适合的使用场景

1. 需要在内存里为一份 JSON/POJO 集合建多维倒排索引（例如 OTA 的航班/车次缓存）。
2. 字段值重复度高，想用字典压缩换内存。
3. 查询条件是少量字段的组合，且希望同时支持等值 + 范围 + 聚合。
4. 重建成本可接受、增量更新不是核心诉求的场景。

### 9.3 不建议的场景

1. 高并发写入且必须持久化/一致性保证——当前字典非原子、没有序列化。
2. 每条数据有几十个字段都要建索引——内存和查询复杂度都会吃紧。
3. 字段基数非常低又需要频繁 `NE / NIN` —— 这类操作在 Trie 里没有比遍历更好的复杂度。

### 9.4 推荐优先做的改进（按性价比）

1. 修 `testDictReUse` 里 `configuration1` → `configuration2` 的 bug，并给“字典复用”场景补一个端到端断言。
2. 把 `Property.level` 迁到 `NodeManager`，解锁 Property 多 trie 真正复用。
3. `HashMapDict.put` 用 `counter.merge`，`mappingOrCreateNodeKey` 用 `computeIfAbsent`，解决并发写丢计数 / 丢 nodeKey。
4. 统一 `erase(Criteria)` 的语义（返回值或改方法名），同时给 `propertySearch` 开放 `Aggregations` 参数。
5. 让 `MapTrie` 构造器支持注入自定义 `NodeManagerFactory`。
6. 恢复或删除 `codec` 里的序列化实现。

## 十、阶段一优化落地记录

针对 6.1 / 6.2 提到的查询路径上的 CPU / GC 热点，已完成一轮内部优化，**公共 API 零侵入**。改动集中在 `BasicOperates` / `DefaultNodeManager` / `MapTrie` 三处，新增一个 `FilteredChildMap` 工具类。

### 10.1 改动清单

**P1-1 · NAVIGABLE_MAP 范围查询返回子视图**

`@d:\Develop\Personal\category_project\summer-trie\src\main\java\top\chitucao\summerframework\trie\operation\BasicOperates.java:250-311` 的 `GT / GTE / LT / LTE / BETWEEN` 五个算子，去掉 `NodeFactoryRegistry.newChildren(result)` 的 O(m log m) 拷贝，直接返回 `NavigableMap.tailMap / headMap / subMap` 视图。百万数据的范围查询单次开销从 "O(log n) 定位 + O(m log m) 拷贝" 降到 "O(log n) 定位 + 零拷贝"。

**P1-1 配套 · 写路径显式快照**

因 view 的写操作会影响底层（甚至不支持），三处写路径必须快照：

- `@d:\Develop\Personal\category_project\summer-trie\src\main\java\top\chitucao\summerframework\trie\nodemanager\DefaultNodeManager.java:140-170` 的 `slice` / `remove`：前者用 `NodeFactoryRegistry.newChildren(result)` 强制物化，后者 `new ArrayList<>(search(...).keySet())`。
- `@d:\Develop\Personal\category_project\summer-trie\src\main\java\top\chitucao\summerframework\trie\MapTrie.java:153-160` 的 `doNormErase` 最后一层：`new HashSet<>(search(...).keySet())` 快照后再删。
- `@d:\Develop\Personal\category_project\summer-trie\src\main\java\top\chitucao\summerframework\trie\MapTrie.java:164-186` 的 `doNormErase` 非最后一层：把原来基于 `iterator.remove()` 的逻辑改成先 `ArrayList<Entry>` 快照，再通过 `cur.children().remove(key)` 显式删除。

**P1-2 · FilteredChildMap 替代 HashMap 分支中间 Map**

新增 `@d:\Develop\Personal\category_project\summer-trie\src\main\java\top\chitucao\summerframework\trie\utils\FilteredChildMap.java` 作为只读懒视图 `Map`。内部只持有源 `Map` 引用 + `Predicate<Map.Entry>`；`entrySet / values / keySet` 均返回懒 iterator，`size / isEmpty` 延迟计算并缓存；所有写方法抛 `UnsupportedOperationException`。

`BasicOperates` 中 HASH_MAP 分支 `NE / GT / GTE / LT / LTE / BETWEEN / IN 多值 / NIN` 共 8 个 O(n) 算子改为返回 `FilteredChildMap`，不再每次 `new HashMap()` + `for put`。`IN 单值` 仍走原物化短路（O(1) get）。

查询链上同一层多条件顺序过滤时，中间结果从 "每层分配一个 HashMap" 降到 "每层套一层 view 壳子"，百万数据 × 5 字段 AND 的中间 Map 分配从 O(5 × 命中数) 降到 O(5)。

**P1-3 · Stream.flatMap 换成 List + for**

- `@d:\Develop\Personal\category_project\summer-trie\src\main\java\top\chitucao\summerframework\trie\MapTrie.java:347-395` 的 `levelSearch` 两段 Stream 链全部换成 `ArrayList<Node<?>>` + `for` 迭代。
- `@d:\Develop\Personal\category_project\summer-trie\src\main\java\top\chitucao\summerframework\trie\MapTrie.java:195-217` 的 `fastErase` 同样换成 `List + for`。

消除每层的 Stream 对象、Supplier 装箱和 lambda capture 分配。

### 10.2 新增测试

- `@d:\Develop\Personal\category_project\summer-trie\src\test\java\top\chitucao\summerframework\trie\utils\FilteredChildMapTest.java` 共 7 个契约测试，覆盖 `size / isEmpty / get / containsKey / 迭代顺序 / 写方法 UOE / 懒求值`。
- `@d:\Develop\Personal\category_project\summer-trie\src\test\java\top\chitucao\summerframework\trie\TrieRegressionTest.java` 共 9 个回归测试，覆盖范围视图类型、`normErase` / `fastErase` 不 CME、两种删除模式结果等价、`FilteredChildMap` 只出现在 HASH_MAP 多值路径、多条件结果与 stream 过滤一致等关键语义不变量。

### 10.3 验证结果

- `mvn -o test -Dtest=TrieTest,TrieRegressionTest,FilteredChildMapTest` → **36 / 36 passed**（原 20 + 新增 16）。
- 所有改动保持公共 API 签名不变，用户代码无需改动即可升级。

### 10.4 未完成 / 下一步

- JMH 基准 benchmark（阶段一 plan 里的 M1.0 / M1.5）暂未落地，等用户确认是否引入 `jmh-core` 到 test scope 后补齐。
- 阶段二（Bitmap 混合方案）和 IN/NIN primitive 化、字典基数自动排序等推迟到 M2。

