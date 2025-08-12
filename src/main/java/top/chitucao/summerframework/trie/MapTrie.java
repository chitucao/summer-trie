package top.chitucao.summerframework.trie;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.InvalidProtocolBufferException;

import top.chitucao.summerframework.trie.codec.MapTrieProtoBuf;
import top.chitucao.summerframework.trie.configuration.Configuration;
import top.chitucao.summerframework.trie.configuration.property.Property;
import top.chitucao.summerframework.trie.configuration.property.SimpleProperty;
import top.chitucao.summerframework.trie.dict.Dict;
import top.chitucao.summerframework.trie.node.Node;
import top.chitucao.summerframework.trie.nodemanager.DefaultNodeManagerFactory;
import top.chitucao.summerframework.trie.nodemanager.NodeManager;
import top.chitucao.summerframework.trie.nodemanager.NodeManagerFactory;
import top.chitucao.summerframework.trie.query.*;
import top.chitucao.summerframework.trie.utils.Pair;

/**
 * 映射实现的字典树
 *
 * @author chitucao
 */
public class MapTrie<T> implements Trie<T> {

    /**
     * 根节点
     */
    private final Node                           root;

    /**
     * 配置
     */
    private final Configuration                  configuration;

    /**
     * 节点管理器双向链表
     */
    private final LinkedList<NodeManager<T, ?>>  nodeManagers;

    /**
     * 节点管理器名称映射
     */
    private final Map<String, NodeManager<T, ?>> nodeManagerNameMap;

    public MapTrie(Configuration configuration) {
        checkAndResolveConfiguration(configuration);
        this.configuration = configuration;
        NodeManagerFactory<T> nodeManagerFactory = new DefaultNodeManagerFactory<>(configuration);
        this.nodeManagers = nodeManagerFactory.createNodeManagers();
        this.nodeManagerNameMap = nodeManagers.stream().collect(Collectors.toMap(m -> m.property().name(), Function.identity()));
        this.root = headNodeManager().createNewNode();
    }

    /**
     * 深度
     * 从0开始，e.g. 8个字段，depth对应7
     *
     * @return 深度
     */
    @Override
    public int getDepth() {
        return tailNodeManager().property().level();
    }

    /**
     * 数据总量
     * -1.返回的是最后一层的数据总量
     *
     * @return 数据总量
     */
    @Override
    public int getSize() {
        if (configuration.isUseFastErase()) {
            return doGetSize(root, 0, getDepth());
        } else {
            return tailNodeManager().property().dict().getSize();
        }
    }

    /**
     * 插入数据
     *
     * @param t 数据
     */
    @Override
    public void insert(T t) {
        Node cur = root;
        for (NodeManager<T, ?> nodeManager : nodeManagers) {
            cur = nodeManager.addChildNode(cur, t);
        }
    }

    /**
     * 删除数据
     * -1.快速删除模式下返回-1
     *
     * @param criteria 删除条件
     * @return 删除的数据条数
     */
    @Override
    public int erase(Criteria criteria) {
        criteriaNotNullCheck(criteria);
        if (configuration.isUseFastErase()) {
            fastErase(criteria);
            return -1;
        } else {
            return normErase(criteria);
        }
    }

    /**
     * 普通删除
    * -1.会维护size变量（getSize的效率比较高）和同步修改dict，需要遍历到最后一层拿到具体的数据，删除效率相对低点；
     * 
     * @param criteria  删除条件
     */
    private int normErase(Criteria criteria) {
        return Math.abs(doNormErase(root, headNodeManager(), criteria.getAllCriterion()));
    }

    /**
    * 普通删除
    
    * @param cur 当前节点
    * @param nodeManager    当前节点管理器
    * @param criterionMap   查询条件
    * @return   当前节点删除的数据总量，绝对值表示删除数量，负数表示子节点完全删除
    */
    private int doNormErase(Node cur, NodeManager<T, ?> nodeManager, Map<String, Criterion> criterionMap) {
        String propertyName = nodeManager.property().name();
        Criterion criterion = criterionMap.get(propertyName);

        // 走到这里说明到最后一层了，可以向上删除了
        // 最后一层可以直接删除字典key，因为都是唯一的
        if (Objects.isNull(nodeManager.next())) {
            if (Objects.isNull(criterion)) {
                Set<Number> dictKeySet = cur.keys();
                for (Number dictKey : dictKeySet) {
                    nodeManager.property().dict().removeDictKey(dictKey);
                }
                // 负数表示完全删除子节点，所以父节点中这个key也可以删除了
                return -dictKeySet.size();
            } else {
                Set<Number> dictKeySet = nodeManager.search(cur, criterion).keySet();
                for (Number dictKey : dictKeySet) {
                    nodeManager.property().dict().removeDictKey(dictKey);
                    cur.removeChild(dictKey);
                }
                return cur.getSize() == 0 ? -dictKeySet.size() : dictKeySet.size();
            }
        }

        Map<Number, Node> childMap = Objects.isNull(criterion) ? cur.childMap() : nodeManager.search(cur, criterion);
        int sum = 0;
        boolean allChildRemoved = true;
        Iterator<Map.Entry<Number, Node>> iterator = childMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Number, Node> entry = iterator.next();
            int removeCount = doNormErase(entry.getValue(), nodeManager.next(), criterionMap);
            if (removeCount >= 0) {
                allChildRemoved = false;
            } else {
                iterator.remove();
            }
            int removeCountAbs = Math.abs(removeCount);
            nodeManager.property().dict().decrDictCount(entry.getKey(), removeCountAbs);
            sum += removeCountAbs;
        }
        return allChildRemoved ? -sum : sum;
    }

    /**
     * 快速删除
     * -1.不会维护size变量和同步修改dict，但是删除效率比较高（不用遍历到最后一层拿到具体的数据），适用于前缀树每次更新是刷新重建的情况；
     *
     * @param criteria 删除条件
     */
    private void fastErase(Criteria criteria) {
        Map<String, Criterion> criterionMap = criteria.getAllCriterion();
        int maxCriteriaLevel = getMaxCriteriaLevel(criterionMap);
        Iterator<NodeManager<T, ?>> iterator = nodeManagers.iterator();

        Stream<Node> cur = Stream.of(root);
        for (int i = 0; i < maxCriteriaLevel; i++) {
            NodeManager<T, ?> nodeManager = iterator.next();
            cur = cur.flatMap(e -> nodeManager.search(e, criterionMap.get(nodeManager.property().name())).values().stream());
        }
        NodeManager<T, ?> lastNodemanager = iterator.next();
        cur.forEach(e -> lastNodemanager.remove(e, criterionMap.get(lastNodemanager.property().name())));
    }

    /**
     * 删除数据
     *
     * @param t 数据
     */
    @Override
    public void erase(T t) {
        Node cur = root;
        NodeManager<T, ?> nodeManager = headNodeManager();
        // 用栈记录每一层节点的父节点
        Stack<Node> parentNodeStack = new Stack<>();
        // 查找该节点，直到最后一层
        while (Objects.nonNull(nodeManager)) {
            Node childNode = nodeManager.findChildNode(cur, t);
            if (Objects.isNull(childNode)) {
                return;
            }
            nodeManager = nodeManager.next();
            parentNodeStack.push(cur);
            cur = childNode;
        }
        if (Objects.isNull(cur)) {
            return;
        }

        // 删除节点
        nodeManager = tailNodeManager();
        while (Objects.nonNull(nodeManager)) {
            Node parent = parentNodeStack.pop();
            // 从最后一层开始删除
            nodeManager.removeChildNode(parent, t);
            // 如果删除后父节点的孩子节点为空，需要继续删除，否则只要删除这一层就好
            if (parent.getSize() != 0) {
                break;
            }
            nodeManager = nodeManager.prev();
        }

        // 删除字典
        nodeManager = headNodeManager();
        while (Objects.nonNull(nodeManager.next())) {
            nodeManager.property().dict().decrDictCount(nodeManager.mappingDictKey(t), 1);
            nodeManager = nodeManager.next();
        }
        nodeManager.property().dict().removeDictKey(nodeManager.mappingDictKey(t));
    }

    /**
     * 是否包含
     *
     * @param criteria 查询条件
     */
    @Override
    public boolean contains(Criteria criteria) {
        criteriaNotNullCheck(criteria);
        Map<String, Criterion> criterion = criteria.getAllCriterion();

        int maxCriteriaLevel = getMaxCriteriaLevel(criteria.getAllCriterion());
        return !this.levelSearch(criterion, new Aggregations(), maxCriteriaLevel).isEmpty();
    }

    /**
     * 是否包含某个数据
     *
     * @param t 数据
     * @return 是否包含
     */
    @Override
    public boolean contains(T t) {
        Node cur = root;
        for (NodeManager<T, ?> nodeManager : nodeManagers) {
            Node childNode = nodeManager.findChildNode(cur, t);
            if (Objects.isNull(childNode)) {
                return false;
            }
            cur = childNode;
        }
        return true;
    }

    /**
     * 数据查询
     * 适用于叶子节点存储数据的情况
     *
     * @param criteria 查询条件
     * @return 数据列表
     */
    @Override
    public List<T> dataSearch(Criteria criteria) {
        if (!configuration.isLeafNodeAsDataNode()) {
            // 如果叶子节点没有存储数据，则不支持这个方法
            throw new IllegalStateException("Leaf node is not a data node, Data search is not supported");
        }
        Map<String, Criterion> criterionMap = getCriterionMap(criteria);
        //noinspection unchecked
        return (List<T>) tailNodeManager().mappingDictValues(levelSearch(criterionMap, new Aggregations(), tailNodeManager().property().level())).collect(Collectors.toList());
    }

    /**
     * 单层查询
     *
     * @param criteria 查询条件
     * @param property 展示层级字段
     * @param <R>      字段数据类型
     * @return 该层级字段列表
     */
    @Override
    public <R> List<R> propertySearch(Criteria criteria, String property) {
        propertyCheck(property);
        Map<String, Criterion> criterionMap = getCriterionMap(criteria);
        //noinspection unchecked
        return (List<R>) nodeManagerNameMap.get(property).mappingDictValues(levelSearch(criterionMap, new Aggregations(), nodeManagerNameMap.get(property).property().level()))
            .collect(Collectors.toList());
    }

    /**
     * 查询某一层的数据
     * 尽量使用遍历操作代替递归操作，提交查询效率
     *
     * @param criterionMap      查询条件
     * @param aggregations      聚合条件
     * @param level             展示字段层级
     * @return 展示层级的字典key
     */
    private Set<Number> levelSearch(Map<String, Criterion> criterionMap, Aggregations aggregations, int level) {
        Iterator<NodeManager<T, ?>> iterator = nodeManagers.iterator();
        Map<String, Aggregation> aggregationMap = aggregations.getAggregationMap();
        // 1.查询到展示字段前
        Stream<Node> cur = Stream.of(root);
        for (int i = 0; i < level; i++) {
            NodeManager<T, ?> nodeManager = iterator.next();
            String propertyName = nodeManager.property().name();
            cur = cur.flatMap(e -> nodeManager.searchAndAgg(e, criterionMap.get(propertyName), aggregationMap.get(propertyName)).values().stream());
        }

        // 2.查询展示字段
        NodeManager<T, ?> levelManager = iterator.next();
        String levelPropertyName = levelManager.property().name();
        Stream<Map<Number, Node>> curChildMap = cur.map(node -> levelManager.searchAndAgg(node, criterionMap.get(levelPropertyName), aggregationMap.get(levelPropertyName)))
            .filter(e -> !e.isEmpty());

        // 3.判断是否还有展示字段后续字段的过滤条件
        int maxCriteriaLevel = this.getMaxCriteriaLevel(criterionMap);
        if (levelManager.property().level() >= maxCriteriaLevel) {
            // 3.1 没有后续字段过滤条件，可以返回了
            return curChildMap.flatMap(childMap -> childMap.keySet().stream()).collect(Collectors.toSet());
        } else {
            // 3.2 还有后续字段过滤条件，判断任意子节点是否满足后续过滤条件
            Set<Number> keys = new HashSet<>();
            NodeManager<T, ?> next = iterator.next();
            curChildMap.forEach(map -> map.forEach((k, v) -> {
                if (!keys.contains(k) && childrenAnyMatch(v, next, criterionMap, aggregationMap, maxCriteriaLevel)) {
                    keys.add(k);
                }
            }));
            return keys;
        }
    }

    /**
     * 节点或子孙节点，有任意一个节点满足后面的查询条件
     *
     * @param node             节点
     * @param nodeManager      节点管理器
     * @param criteriaMap      查询条件
     * @param aggregationMap   聚合条件
     * @param maxCriteriaLevel 查询条件最大深度
     * @return 有任意一个子孙节点满足后面的查询条件
     */
    private boolean childrenAnyMatch(Node node, NodeManager<T, ?> nodeManager, Map<String, Criterion> criteriaMap, Map<String, Aggregation> aggregationMap, int maxCriteriaLevel) {
        String propertyName = nodeManager.property().name();
        Criterion criterion = criteriaMap.get(propertyName);
        // 终止条件，走到最后一个查询条件
        if (nodeManager.property().level() == maxCriteriaLevel) {
            return nodeManager.contains(node, criterion);
        }
        Stream<Node> cur = Stream.of(node);
        cur = cur.flatMap(e -> nodeManager.searchAndAgg(e, criterion, aggregationMap.get(propertyName)).values().stream());

        return cur.anyMatch(childNode -> this.childrenAnyMatch(childNode, nodeManager.next(), criteriaMap, aggregationMap, maxCriteriaLevel));
    }

    /**
     * 多层列表查询
     * -1.可以指定多个层级的字段，并将查询结果树平铺成一个列表后返回；
     * -2.支持对字段进行聚合；
     *
     * @param criteria      查询条件
     * @param aggregations  字段聚合条件
     * @param resultBuilder 结果构建器
     * @return 数据列表
     */
    @Override
    public <E> List<E> listSearch(Criteria criteria, Aggregations aggregations, ResultBuilder<E> resultBuilder) {
        resultBuilderCheck(resultBuilder);
        Map<String, Criterion> criterionMap = getCriterionMap(criteria);

        List<List<Number>> dataList = multiLevelSearch(criterionMap, aggregations, resultBuilder.getSetterMap().keySet().toArray(new String[0]));
        if (dataList.isEmpty()) {
            return new ArrayList<>();
        }
        List<E> result = new ArrayList<>();
        @SuppressWarnings("rawtypes")
        List<Pair<NodeManager, BiConsumer>> propertySetterList = getPropertySetterList(resultBuilder);
        for (List<Number> fields : dataList) {
            E data = resultBuilder.getSupplier().get();
            for (int i = 0; i < fields.size(); i++) {
                @SuppressWarnings("rawtypes")
                Pair<NodeManager, BiConsumer> pair = propertySetterList.get(i);
                try {
                    //noinspection unchecked
                    pair.getValue().accept(data, pair.getKey().property().dict().getDictValue(fields.get(i)));
                } catch (Exception e) {
                    // set方法还是很容易出错的，比如类型转换异常，这里打印下具体异常的字段
                    throw new RuntimeException("Property value set failed. Property name is " + pair.getKey().property().name() + ". Error msg: " + e.getMessage(), e.getCause());
                }
            }
            result.add(data);
        }
        return result;
    }

    /**
     * 子树查询
     * -1.指定查询条件和需要展示的字段，返回前缀树的子树视图
     * -2.只查询一个字段返回去重后list，多个字段返回hashmap，hashmap，是一个树结构；
     *
     * @param criteria   查询条件
     * @param properties 展示字段
     * @return 基于查询条件和展示字段构建的子树
     */
    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Object treeSearch(Criteria criteria, Aggregations aggregations, String... properties) {
        if (properties == null || properties.length == 0) {
            return new ArrayList<>();
        }

        if (properties.length == 1) {
            return propertySearch(criteria, properties[0]);
        }

        Map<String, Criterion> criterionMap = getCriterionMap(criteria);
        List<List<Number>> dataList = multiLevelSearch(criterionMap, aggregations, properties);
        if (dataList.isEmpty()) {
            return new HashMap<>();
        }

        List<NodeManager> propertyNodeManagerList = getPropertyNodeManagerList(properties);

        Map result = new HashMap<>();
        for (List<Number> fields : dataList) {
            Object cur = result;
            for (int i = 0; i < properties.length; i++) {
                Object dictValue = propertyNodeManagerList.get(i).property().dict().getDictValue(fields.get(i));
                if (i < properties.length - 1) {
                    // 非最后一层是map
                    Map map = (Map) cur;
                    if (!map.containsKey(dictValue)) {
                        if (i == properties.length - 2) {
                            map.put(dictValue, new ArrayList<>());
                        } else {
                            map.put(dictValue, new HashMap<>());
                        }
                    }
                    cur = map.get(dictValue);
                } else {
                    // 最后一层是list
                    List list = (List) cur;
                    list.add(dictValue);
                }
            }
        }
        return result;
    }

    /**
     * 多层列表查询
     * 这里返回的是树节点数据
     *
     * @param criterionMap      查询条件
     * @param aggregations      聚合条件
     * @param properties        要展示的字段列表
     * @return 平铺的列表数据
     */
    private List<List<Number>> multiLevelSearch(Map<String, Criterion> criterionMap, Aggregations aggregations, String... properties) {
        if (properties == null || properties.length == 0) {
            return new ArrayList<>();
        }
        propertiesCheck(properties);
        sortProperties(properties);

        if (Objects.isNull(aggregations)) {
            aggregations = new Aggregations();
        }
        aggregationCheck(aggregations);

        int maxCriteriaLevel = this.getMaxCriteriaLevel(criterionMap);
        int maxPropertyLevel = this.getMaxPropertyLevel(properties);

        List<List<Number>> result = new ArrayList<>();
        dfsSearch(this.root, headNodeManager(), criterionMap, maxCriteriaLevel, new HashSet<>(Arrays.asList(properties)), maxPropertyLevel, aggregations.getAggregationMap(),
            result, new ArrayList<>());

        return result;
    }

    /**
     * 获取满足查询条件的查询字段组成的所有路径
     *
     * @param cur              当前节点
     * @param nodeManager      节点管理器
     * @param criteriaMap      查询条件名称映射
     * @param maxCriteriaLevel 查询条件最大深度
     * @param propertySet      展示字段集合
     * @param maxPropertyLevel 展示字段最大深度
     * @param result           路径列表
     * @param path             路径
     */
    private void dfsSearch(Node cur, NodeManager<T, ?> nodeManager, Map<String, Criterion> criteriaMap, int maxCriteriaLevel, Set<String> propertySet, int maxPropertyLevel,
                           Map<String, Aggregation> aggregationMap, List<List<Number>> result, List<Number> path) {
        int level = nodeManager.property().level();
        String propertyName = nodeManager.property().name();
        if (level > maxCriteriaLevel && level > maxPropertyLevel) {
            result.add(path);
            return;
        }
        Criterion criterion = criteriaMap.get(propertyName);
        Aggregation aggregation = aggregationMap.get(propertyName);
        Map<Number, Node> map = nodeManager.searchAndAgg(cur, criterion, aggregation);
        boolean isResultProperty = propertySet.contains(propertyName);
        for (Map.Entry<Number, Node> entry : map.entrySet()) {
            List<Number> newPath = new ArrayList<>(path);
            if (isResultProperty) {
                newPath.add(entry.getKey());
            }
            if (Objects.isNull(nodeManager.next())) {
                result.add(newPath);
                return;
            }
            dfsSearch(entry.getValue(), nodeManager.next(), criteriaMap, maxCriteriaLevel, propertySet, maxPropertyLevel, aggregationMap, result, newPath);
        }
    }

    /**
     * 查询某个字段的对应的字典值
     *
     * @param property 查询字段
     * @param dictKeys 字典key列表
     * @return 该字段所有字典值
     */
    @Override
    public <R> Set<R> dictValues(String property, Number... dictKeys) {
        propertyCheck(property);
        if (dictKeys == null || dictKeys.length == 0) {
            //noinspection unchecked
            return (Set<R>) nodeManagerNameMap.get(property).property().dict().dictValues();
        }
        Map<Number, ?> dictAll = nodeManagerNameMap.get(property).property().dict().dictAll();
        //noinspection unchecked
        return (Set<R>) Arrays.stream(dictKeys).map(dictAll::get).collect(Collectors.toSet());
    }

    /**
     * 所有字段的字典大小
     * 可以在配合压缩数据的时候使用，一般是字典值较小的字段放在前面压缩效率更高，整体可以认为是一个梯形，下底是固定长度，所以上底较小面积最小;
     *
     * @return 每个字段的字典大小
     */
    @Override
    public Map<String, Integer> dictSizes() {
        Map<String, Integer> result = new HashMap<>();
        for (NodeManager<T, ?> nodeManager : nodeManagers) {
            result.put(nodeManager.property().name(), nodeManager.property().dict().getSize());
        }
        return result;
    }

    /**
     * 序列化
     *
     * @return 字节数组
     */
    @Override
    public byte[] serialize() {
        MapTrieProtoBuf.Node rootNode = buildToProtoBufNode(MapTrieProtoBuf.Node.newBuilder(), this.root, 0, getDepth(), MapTrieProtoBuf.Node.newBuilder().build());
        List<MapTrieProtoBuf.Dict> dictList = buildToProtoBufDictList();
        MapTrieProtoBuf.Trie trie = MapTrieProtoBuf.Trie.newBuilder().setRoot(rootNode).addAllDict(dictList).build();
        return trie.toByteArray();
    }

    /**
     * 构建成序列化节点
     *
     * @param protoBufCur    当前节点构建器
     * @param cur            当前节点
     * @param level          当前层级
     * @param depth          最大深度
     * @param emptyValueNode 空值节点
     * @return 序列化节点
     */
    private MapTrieProtoBuf.Node buildToProtoBufNode(MapTrieProtoBuf.Node.Builder protoBufCur, Node cur, int level, int depth, MapTrieProtoBuf.Node emptyValueNode) {
        if (level == depth) {
            for (Map.Entry<Number, Node> entry : cur.childMap().entrySet()) {
                protoBufCur.putChild(entry.getKey().longValue(), emptyValueNode);
            }
            return protoBufCur.build();
        }
        int nextLevel = level + 1;
        for (Map.Entry<Number, Node> entry : cur.childMap().entrySet()) {
            protoBufCur.putChild(entry.getKey().longValue(), buildToProtoBufNode(MapTrieProtoBuf.Node.newBuilder(), entry.getValue(), nextLevel, depth, emptyValueNode));
        }
        return protoBufCur.build();
    }

    /**
     * 构建成序列化字典列表
     *
     * @return 序列化字典列表
     */
    private List<MapTrieProtoBuf.Dict> buildToProtoBufDictList() {
        List<MapTrieProtoBuf.Dict> dictList = new ArrayList<>();

        for (NodeManager<T, ?> nodeManager : nodeManagers) {
            Dict<?> dict = nodeManager.property().dict();
            MapTrieProtoBuf.Dict.Builder dictBuilder = MapTrieProtoBuf.Dict.newBuilder();
            if (dict.getSize() == 0) {
                dictBuilder.setKeyClazz(Long.class.getName()).setValClazz(Object.class.getName());
            } else {
                dictBuilder.setKeyClazz(dict.dictAll().keySet().stream().findAny().map(r -> r.getClass().getName()).orElse(null))
                    .setValClazz(dict.dictAll().values().stream().findAny().map(r -> r.getClass().getName()).orElse(null));
            }
            dictBuilder.addAllMapEntry(buildToProtoBufDictEntryList(dict.dictAll()));
            dictList.add(dictBuilder.build());
        }
        return dictList;
    }

    /**
    * 构建成序列化字典值列表
    *
    * @return 构建成序列化字典值列表
    */
    private List<MapTrieProtoBuf.MapEntry> buildToProtoBufDictEntryList(Map<Number, ?> numberMap) {
        ObjectMapper objectMapper = new ObjectMapper();
        List<MapTrieProtoBuf.MapEntry> entryList = new ArrayList<>();
        for (Map.Entry<Number, ?> entry : numberMap.entrySet()) {
            try {
                entryList.add(MapTrieProtoBuf.MapEntry.newBuilder().setKey(entry.getKey().longValue()).setVal(objectMapper.writeValueAsString(entry.getValue())).build());
            } catch (JsonProcessingException e) {
                throw new IllegalStateException(e);
            }
        }
        return entryList;
    }

    @Override
    public void deserialize(byte[] bytes) {
        MapTrieProtoBuf.Trie trie;
        try {
            trie = MapTrieProtoBuf.Trie.parseFrom(bytes);
        } catch (InvalidProtocolBufferException e) {
            throw new IllegalStateException(e);
        }
        List<Pair<Function<Number, Number>, Function<String, Object>>> dictMapperList = getDictMapperList(trie.getDictList());
        // 构建节点数据
        buildFromProtoBufNode(this.root, trie.getRoot(), headNodeManager(), dictMapperList);
        // 构建字典数据
        buildFromProtoBufDict(trie.getDictList(), dictMapperList);
    }

    /**
     * 从ProtoBuf构建字典
     *
     * @param dictList       字典列表
     * @param dictMapperList 字典键值映射方法
     */
    @SuppressWarnings("rawtypes")
    private void buildFromProtoBufDict(List<MapTrieProtoBuf.Dict> dictList, List<Pair<Function<Number, Number>, Function<String, Object>>> dictMapperList) {
        for (NodeManager<T, ?> nodeManager : nodeManagers) {
            int level = nodeManager.property().level();
            Dict<?> dict = nodeManager.property().dict();
            Pair<Function<Number, Number>, Function<String, Object>> pair = dictMapperList.get(level);
            for (MapTrieProtoBuf.MapEntry entry : dictList.get(level).getMapEntryList()) {
                dict.putDictObj(pair.getKey().apply(entry.getKey()), pair.getValue().apply(entry.getVal()));
            }
            if (nodeManager.property() instanceof SimpleProperty) {
                SimpleProperty property = (SimpleProperty) nodeManager.property();
                Long curId = dict.dictAll().keySet().stream().map(Number::longValue).max(Comparator.naturalOrder()).orElse(0L);
                property.getDictKeyAdder().setId(curId);
            }
        }
    }

    /**
     * 从ProtoBuf构建节点
     *
     * @param cur            当前节点
     * @param protoBufCur    待反序列化的protoBuf当前节点
     * @param nodeManager    节点管理器
     * @param dictMapperList 字典键值映射方法列表
     */
    private void buildFromProtoBufNode(Node cur, MapTrieProtoBuf.Node protoBufCur, NodeManager<T, ?> nodeManager,
                                       List<Pair<Function<Number, Number>, Function<String, Object>>> dictMapperList) {
        Function<Number, Number> dictKeyMapper = dictMapperList.get(nodeManager.property().level()).getKey();
        NodeManager<T, ?> nextNodeManager = nodeManager.next();
        if (nextNodeManager == null) {
            cur.setChild(nodeManager.createEmptyValueNode(protoBufCur.getChildMap().keySet().stream().map(dictKeyMapper)).childMap());
            return;
        }
        for (Map.Entry<Long, MapTrieProtoBuf.Node> entry : protoBufCur.getChildMap().entrySet()) {
            cur.addChild(entry.getKey(), nextNodeManager.createNewNode());
            buildFromProtoBufNode(cur.getChild(entry.getKey()), entry.getValue(), nextNodeManager, dictMapperList);
        }
    }

    /**
     * 获取字典键值的映射方法
     *
     * @param protoBufDictList 待反序列化的protoBuf字典
     * @return 字典键值映射方法列表
     */
    private List<Pair<Function<Number, Number>, Function<String, Object>>> getDictMapperList(List<MapTrieProtoBuf.Dict> protoBufDictList) {
        List<Pair<Function<Number, Number>, Function<String, Object>>> result = new ArrayList<>();
        for (MapTrieProtoBuf.Dict protoBufDict : protoBufDictList) {
            result.add(new Pair<>(getDictKeyMapper(protoBufDict.getKeyClazz()), getDictValMapper(protoBufDict.getValClazz())));
        }
        return result;
    }

    /**
     * 获取字典值的映射方法
     *
     * @param dictValClazzName 字典val类全路径名
     * @return 字典键值对的映射方法
     */
    private Function<String, Object> getDictValMapper(String dictValClazzName) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Class<?> dictValClazz = Class.forName(dictValClazzName);
            return e -> {
                try {
                    return objectMapper.readValue(e, dictValClazz);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            };
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取字典key的映射方法
     *
     * @param dictKeyClazzName 字典key类全路径名
     * @return 字典key的映射方法
     */
    private Function<Number, Number> getDictKeyMapper(String dictKeyClazzName) {
        if (Objects.equals(dictKeyClazzName, Byte.class.getName())) {
            return Number::intValue;
        } else if (Objects.equals(dictKeyClazzName, Short.class.getName())) {
            return Number::shortValue;
        } else if (Objects.equals(dictKeyClazzName, Integer.class.getName())) {
            return Number::intValue;
        } else {
            return Number::longValue;
        }
    }

    /**
     * 获取查询条件
     * 
     * @param criteria  条件
     * @return          所有查询条件
     */
    private static Map<String, Criterion> getCriterionMap(Criteria criteria) {
        return Objects.isNull(criteria) ? new HashMap<>() : criteria.getAllCriterion();
    }

    /**
     * 获取查询字段的最大深度
     *
     * @param properties 查询字段列表
     * @return 查询字段列表的最大深度
     */
    private int getMaxPropertyLevel(String... properties) {
        return nodeManagerNameMap.get(properties[properties.length - 1]).property().level();
    }

    /**
     * 获取查询条件的最大深度
     *
     * @param criterionMap 查询条件
     * @return 查询条件的最大深度
     */
    private int getMaxCriteriaLevel(Map<String, Criterion> criterionMap) {
        if (criterionMap.isEmpty()) {
            return -1;
        }
        Set<String> keys = criterionMap.keySet();
        NodeManager<T, ?> tNodeManager = tailNodeManager();
        while (tNodeManager != null) {
            if (keys.contains(tNodeManager.property().name())) {
                return tNodeManager.property().level();
            }
            tNodeManager = tNodeManager.prev();
        }
        return -1;
    }

    /**
     * 获取字段构建方法列表
     *
     * @param resultBuilder 结果构建器
     * @param <E>           结果类型
     * @return 字段构建方法列表
     */
    @SuppressWarnings("rawtypes")
    private <E> List<Pair<NodeManager, BiConsumer>> getPropertySetterList(ResultBuilder<E> resultBuilder) {
        List<Pair<NodeManager, BiConsumer>> propertySetterList = new ArrayList<>();
        for (NodeManager<T, ?> nodeManager : nodeManagers) {
            String propertyName = nodeManager.property().name();
            if (resultBuilder.getSetterMap().containsKey(propertyName)) {
                propertySetterList.add(new Pair<>(nodeManager, resultBuilder.getSetterMap().get(propertyName)));
            }
        }
        return propertySetterList;
    }

    /**
     * 获取展示字段对应的节点管理器列表
     *
     * @param properties 展示字段
     * @return 展示字段对应的节点管理器列表
     */
    @SuppressWarnings("rawtypes")
    private List<NodeManager> getPropertyNodeManagerList(String... properties) {
        HashSet<String> propertySet = new HashSet<>(Arrays.asList(properties));

        List<NodeManager> propertyNodeManagerList = new ArrayList<>();
        for (NodeManager<T, ?> nodeManager : nodeManagers) {
            String propertyName = nodeManager.property().name();
            if (propertySet.contains(propertyName)) {
                propertyNodeManagerList.add(nodeManager);
            }
        }
        return propertyNodeManagerList;
    }

    /**
     * 字段排序
     *
     * @param properties 字段列表
     */
    private void sortProperties(String... properties) {
        Arrays.sort(properties, Comparator.comparing(p -> nodeManagerNameMap.get(p).property().level()));
    }

    /**
     * 头部节点管理器
     *
     * @return 头部节点管理器
     */
    private NodeManager<T, ?> headNodeManager() {
        return nodeManagers.getFirst();
    }

    /**
     * 尾部节点管理器
     *
     * @return 尾部节点管理器
     */
    private NodeManager<T, ?> tailNodeManager() {
        return nodeManagers.getLast();
    }

    /**
    * 数据总量
    *
    * @param cur      当前节点
    * @param depth    当前深度
    * @param maxDepth 最大深度
    * @return 数据总量
    */
    private int doGetSize(Node cur, int depth, int maxDepth) {
        if (depth == maxDepth) {
            return cur.getSize();
        }
        int sumSize = 0;
        for (Node child : cur.childMap().values()) {
            sumSize += doGetSize(child, depth + 1, maxDepth);
        }
        return sumSize;
    }

    /**
     * 配置校验和处理
     *
     * @param configuration 配置
     */
    private void checkAndResolveConfiguration(Configuration configuration) {
        if (configuration.getProperties().isEmpty()) {
            throw new IllegalStateException("Properties are empty");
        }
        configuration.setLeafNodeAsDataNode(checkNodeAsDataNode(configuration.getLastProperty()));
    }

    /**
     * 判断节点是否存储数据本身
     *
     * @param property 节点属性
     * @return 节点是否存储数据本身
     */
    @SuppressWarnings("rawtypes")
    private boolean checkNodeAsDataNode(Property property) {
        try {
            Object obj = new Object();
            //noinspection unchecked
            return obj == property.mappingValue(obj);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 查询条件非空校验
     *
     * @param criteria 查询条件
     */
    private void criteriaNotNullCheck(Criteria criteria) {
        if (Objects.isNull(criteria)) {
            throw new IllegalArgumentException("Criteria can not be null");
        }
    }

    /**
     * 聚合条件校验
     *
     * @param aggregations 聚合条件
     */
    private void aggregationCheck(Aggregations aggregations) {
        Map<String, Aggregation> aggregationMap = aggregations.getAggregationMap();
        if (aggregationMap.isEmpty()) {
            return;
        }
        Set<String> propertySet = new HashSet<>();
        for (Map.Entry<String, Aggregation> entry : aggregationMap.entrySet()) {
            if (!nodeManagerNameMap.containsKey(entry.getKey())) {
                throw new IllegalArgumentException(propertyNotDefinedMsg(entry.getKey()));
            }
            if (!propertySet.add(entry.getKey())) {
                throw new IllegalArgumentException("Duplicate property: " + entry.getValue() + " in aggregations");
            }
        }
    }

    /**
     * 结果构建器校验
     *
     * @param resultBuilder 结果构建器
     */
    private <E> void resultBuilderCheck(ResultBuilder<E> resultBuilder) {
        @SuppressWarnings("rawtypes")
        Map<String, BiConsumer> setterMap = resultBuilder.getSetterMap();
        if (setterMap.isEmpty()) {
            throw new IllegalArgumentException("ResultBuilder setter map is empty");
        }
        Set<String> propertySet = new HashSet<>();
        //noinspection rawtypes
        for (Map.Entry<String, BiConsumer> entry : setterMap.entrySet()) {
            if (!nodeManagerNameMap.containsKey(entry.getKey())) {
                throw new IllegalArgumentException(propertyNotDefinedMsg(entry.getKey()));
            }
            if (!propertySet.add(entry.getKey())) {
                throw new IllegalArgumentException("Duplicate property: " + entry.getValue() + " in resultBuilder");
            }
        }
    }

    /**
     * 展示字段校验
     *
     * @param properties 展示字段列表
     */
    private void propertiesCheck(String... properties) {
        if (properties == null || properties.length == 0) {
            return;
        }
        Set<String> propertySet = new HashSet<>();
        for (String property : properties) {
            if (!nodeManagerNameMap.containsKey(property)) {
                throw new IllegalArgumentException(propertyNotDefinedMsg(property));
            }
            if (!propertySet.add(property)) {
                throw new IllegalArgumentException("Duplicate property: " + property + "in properties");
            }
        }
    }

    /**
     * 查询字段校验
     *
     * @param property 查询字段
     */
    private void propertyCheck(String property) {
        if (!nodeManagerNameMap.containsKey(property)) {
            throw new IllegalArgumentException(propertyNotDefinedMsg(property));
        }
    }

    /**
     * 字段未定义
     *
     * @param property 字段
     * @return 异常描述
     */
    private String propertyNotDefinedMsg(String property) {
        return "Property: " + property + " is not defined, Properties: " + nodeManagerNameMap.keySet();
    }
}