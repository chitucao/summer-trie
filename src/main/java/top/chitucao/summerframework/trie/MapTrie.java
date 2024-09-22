package top.chitucao.summerframework.trie;

import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.ZipUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.protobuf.ByteString;
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

import java.util.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 映射实现的字典树
 *
 * @author chitucao(zhonggang.zhu)
 * @version Id: MapTrie.java, v 0.1 2024-08-06 下午4:02 chitucao Exp $$
 */
public class MapTrie<T> implements Trie<T> {

    /**
     * 根节点
     */
    private final Node root;

    /**
     * 记录数据总量
     */
    private LongAdder size;

    /**
     * 配置
     */
    private final Configuration configuration;

    /**
     * 节点管理器双向链表
     */
    private final LinkedList<NodeManager<T, ?>> nodeManagers;

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
     *
     * @return 数据总量
     */
    @Override
    public int getSize() {
        if (configuration.isUseFastErase()) {
            return doGetSize(root, 0, getDepth());
        } else {
            return size.intValue();
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
            if (!configuration.isUseFastErase() && nodeManager.property().isLeaf()) {
                size.increment();
            }
        }
    }

    /**
     * 删除数据
     *
     * @param criteria 删除条件
     */
    @Override
    public void erase(Criteria criteria) {
        if (Objects.isNull(criteria) || criteria.getCriterionList().isEmpty()) {
            return;
        }
        if (configuration.isUseFastErase()) {
            fastErase(criteria);
        } else {
            normErase(criteria);
        }
    }

    /**
     * 普通删除
     * 先找到符合条件的数据，再依次删除，需要最后一个节点存储数据本身
     *
     * @param criteria 删除条件
     */
    private void normErase(Criteria criteria) {
        criteriaCheck(criteria);
        sortCriteria(criteria);
        this.dataSearch(criteria).forEach(this::erase);
    }

    /**
     * 快速删除
     * 不会维护size变量，但是删除效率比较高
     *
     * @param criteria 删除条件
     */
    private void fastErase(Criteria criteria) {
        Map<String, Criterion> criteriaMap = criteria.getCriterionMap();
        int maxCriteriaLevel = this.getMaxCriteriaLevel(criteria);
        Iterator<NodeManager<T, ?>> iterator = nodeManagers.iterator();

        Stream<Node> cur = Stream.of(root);
        for (int i = 0; i < maxCriteriaLevel; i++) {
            NodeManager<T, ?> nodeManager = iterator.next();
            cur = cur.flatMap(e -> nodeManager.search(e, criteriaMap.get(nodeManager.property().name())).values().stream());
        }
        NodeManager<T, ?> lastNodemanager = iterator.next();
        cur.forEach(e -> lastNodemanager.remove(e, criteriaMap.get(lastNodemanager.property().name())));
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
        // 走到这里说明存在该节点，可以删除了
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
        if (!configuration.isUseFastErase()) {
            size.decrement();
        }
    }

    /**
     * 是否包含
     *
     * @param criteria 查询条件
     */
    @Override
    public boolean contains(Criteria criteria) {
        criteriaCheck(criteria);
        sortCriteria(criteria);
        if (criteria.getCriterionList().isEmpty()) {
            return root.getSize() != 0;
        }

        int maxCriteriaLevel = getMaxCriteriaLevel(criteria);
        return !this.levelSearch(criteria, new Aggregations(), maxCriteriaLevel).isEmpty();
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
        criteriaCheck(criteria);
        //noinspection unchecked
        return (List<T>) tailNodeManager().mappingDictValues(levelSearch(criteria, new Aggregations(), tailNodeManager().property().level())).collect(Collectors.toList());
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
        criteriaCheck(criteria);
        propertyCheck(property);
        //noinspection unchecked
        return (List<R>) nodeManagerNameMap.get(property).mappingDictValues(levelSearch(criteria, new Aggregations(), nodeManagerNameMap.get(property).property().level()))
                .collect(Collectors.toList());
    }

    /**
     * 查询某一层的数据
     * 尽量使用遍历操作代替递归操作，提交查询效率
     *
     * @param criteria     查询条件
     * @param aggregations 聚合条件
     * @param level        展示字段层级
     * @return 展示层级的字典key
     */
    private Set<Number> levelSearch(Criteria criteria, Aggregations aggregations, int level) {
        Iterator<NodeManager<T, ?>> iterator = nodeManagers.iterator();
        Map<String, Criterion> criteriaMap = criteria.getCriterionMap();
        Map<String, Aggregation> aggregationMap = aggregations.getAggregationMap();
        // 1.查询到展示层级
        Stream<Node> cur = Stream.of(root);
        for (int i = 0; i < level; i++) {
            NodeManager<T, ?> nodeManager = iterator.next();
            String propertyName = nodeManager.property().name();
            cur = cur.flatMap(e -> nodeManager.searchAndAgg(e, criteriaMap.get(propertyName), aggregationMap.get(propertyName)).values().stream());
        }

        // 2.对展示层级的数据做过滤
        NodeManager<T, ?> levelManager = iterator.next();
        String levelPropertyName = levelManager.property().name();
        Stream<Map<Number, Node>> curChildMap = cur.map(node -> levelManager.searchAndAgg(node, criteriaMap.get(levelPropertyName), aggregationMap.get(levelPropertyName)))
                .filter(e -> !e.isEmpty());

        // 3.判断是否还有展示层级后续层级的过滤条件
        int maxCriteriaLevel = this.getMaxCriteriaLevel(criteria);
        if (levelManager.property().level() >= maxCriteriaLevel) {
            // 3.1 没有后续层级过滤条件，可以返回了
            return curChildMap.flatMap(childMap -> childMap.keySet().stream()).collect(Collectors.toSet());
        } else {
            // 3.2 还有后续层级过滤条件，判断任意子节点是否满足后续过滤条件
            Set<Number> keys = Sets.newHashSet();
            NodeManager<T, ?> next = iterator.next();
            curChildMap.forEach(map -> map.forEach((k, v) -> {
                if (!keys.contains(k) && childrenAnyMatch(v, next, criteriaMap, aggregationMap, maxCriteriaLevel)) {
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
        // 返回条件，走到最后一个查询条件
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
        List<List<Number>> dataList = multiLevelSearch(criteria, aggregations, resultBuilder.getSetterMap().keySet().toArray(new String[0]));
        if (dataList.isEmpty()) {
            return Lists.newArrayList();
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
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Object treeSearch(Criteria criteria, Aggregations aggregations, String... properties) {
        if (properties == null || properties.length == 0) {
            return Lists.newArrayList();
        }

        if (properties.length == 1) {
            return propertySearch(criteria, properties[0]);
        }

        List<List<Number>> dataList = multiLevelSearch(criteria, aggregations, properties);
        if (dataList.isEmpty()) {
            return Maps.newHashMap();
        }

        List<NodeManager> propertyNodeManagerList = getPropertyNodeManagerList(properties);

        Map result = Maps.newHashMap();
        for (List<Number> fields : dataList) {
            Object cur = result;
            for (int i = 0; i < fields.size(); i++) {
                Object dictValue = propertyNodeManagerList.get(i).property().dict().getDictValue(fields.get(i));
                if (i < properties.length) {
                    // 非最后一层是map
                    Map map = (Map) cur;
                    if (!map.containsKey(dictValue)) {
                        if (i == properties.length - 1) {
                            map.put(dictValue, Lists.newArrayList());
                        } else {
                            map.put(dictValue, Maps.newHashMap());
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
     * @param criteria     查询条件
     * @param aggregations 聚合条件
     * @param properties   要展示的字段列表
     * @return 平铺的列表数据
     */
    private List<List<Number>> multiLevelSearch(Criteria criteria, Aggregations aggregations, String... properties) {
        if (properties == null || properties.length == 0) {
            return Lists.newArrayList();
        }
        propertiesCheck(properties);
        sortProperties(properties);

        if (Objects.isNull(criteria)) {
            criteria = new Criteria();
        }
        criteriaCheck(criteria);
        sortCriteria(criteria);

        if (Objects.isNull(aggregations)) {
            aggregations = new Aggregations();
        }
        aggregationCheck(aggregations);

        int maxCriteriaLevel = this.getMaxCriteriaLevel(criteria);
        int maxPropertyLevel = this.getMaxPropertyLevel(properties);

        List<List<Number>> result = Lists.newArrayList();
        dfsSearch(this.root, headNodeManager(), criteria.getCriterionMap(), maxCriteriaLevel, Sets.newHashSet(properties), maxPropertyLevel, aggregations.getAggregationMap(),
                result, Lists.newArrayList());

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
            List<Number> newPath = Lists.newArrayList(path);
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
     * 查询某个字段的所有字典值
     *
     * @param property 查询字段
     * @return 该字段所有字典值
     */
    @Override
    public <R> Set<R> dictValues(String property) {
        propertyCheck(property);
        //noinspection unchecked
        return (Set<R>) nodeManagerNameMap.get(property).property().dict().dictValues();
    }

    /**
     * 所有字段的字典大小
     * 可以在配合压缩数据的时候使用，一般是字典值较小的字段放在前面压缩效率更高，整体可以认为是一个梯形，下底是固定长度，所以上底较小面积最小;
     *
     * @return 每个字段的字典大小
     */
    @Override
    public Map<String, Integer> dictSizes() {
        Map<String, Integer> result = Maps.newHashMap();
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
        MapTrieProtoBuf.Trie trie = MapTrieProtoBuf.Trie.newBuilder().setRoot(rootNode).setSize(Objects.isNull(this.size) ? 0L : this.size.longValue()).addAllDict(dictList)
                .build();
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
     * 构建成序列化字典
     *
     * @return 序列化字典列表
     */
    private List<MapTrieProtoBuf.Dict> buildToProtoBufDictList() {
        List<MapTrieProtoBuf.Dict> dictList = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        for (NodeManager<T, ?> nodeManager : nodeManagers) {
            Dict<?> dict = nodeManager.property().dict();
            MapTrieProtoBuf.Dict.Builder dictBuilder = MapTrieProtoBuf.Dict.newBuilder();
            if (dict.getSize() == 0) {
                dictBuilder.setKeyClazz(Long.class.getName()).setValClazz(Object.class.getName());
            } else {
                dictBuilder.setKeyClazz(dict.dictAll().keySet().stream().findAny().map(r -> r.getClass().getName()).orElse(null))
                        .setValClazz(dict.dictAll().values().stream().findAny().map(r -> r.getClass().getName()).orElse(null));
            }
            try {
                dictBuilder.setKv(ByteString.copyFrom(ZipUtil.gzip(objectMapper.writeValueAsString(dict.dictAll()), "utf-8")));
            } catch (JsonProcessingException e) {
                throw new IllegalStateException(e);
            }
            dictList.add(dictBuilder.build());
        }
        return dictList;
    }

    @Override
    public void deserialize(byte[] bytes) {
        MapTrieProtoBuf.Trie trie;
        try {
            trie = MapTrieProtoBuf.Trie.parseFrom(bytes);
        } catch (InvalidProtocolBufferException | IORuntimeException e) {
            throw new IllegalStateException(e);
        }
        @SuppressWarnings("rawtypes")
        List<Pair<Function<Number, Number>, Function<String, Map>>> dictMapperList = getDictMapperList(trie.getDictList());
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
    private void buildFromProtoBufDict(List<MapTrieProtoBuf.Dict> dictList, List<Pair<Function<Number, Number>, Function<String, Map>>> dictMapperList) {
        for (NodeManager<T, ?> nodeManager : nodeManagers) {
            int level = nodeManager.property().level();
            Dict<?> dict = nodeManager.property().dict();
            Pair<Function<Number, Number>, Function<String, Map>> pair = dictMapperList.get(level);
            Map map = dictMapperList.get(level).getValue().apply(ZipUtil.unGzip(dictList.get(level).getKv().toByteArray(), "utf-8"));
            for (Object obj : map.entrySet()) {
                Map.Entry entry = (Map.Entry) obj;
                dict.putDictObj(pair.getKey().apply((Number) entry.getKey()), entry.getValue());
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
    @SuppressWarnings("rawtypes")
    private void buildFromProtoBufNode(Node cur, MapTrieProtoBuf.Node protoBufCur, NodeManager<T, ?> nodeManager,
                                       List<Pair<Function<Number, Number>, Function<String, Map>>> dictMapperList) {
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
    @SuppressWarnings("rawtypes")
    private List<Pair<Function<Number, Number>, Function<String, Map>>> getDictMapperList(List<MapTrieProtoBuf.Dict> protoBufDictList) {
        List<Pair<Function<Number, Number>, Function<String, Map>>> result = new ArrayList<>();
        for (MapTrieProtoBuf.Dict protoBufDict : protoBufDictList) {
            result.add(new Pair<>(getDictKeyMapper(protoBufDict.getKeyClazz()), getDictValMapper(protoBufDict.getKeyClazz(), protoBufDict.getValClazz())));
        }
        return result;
    }

    /**
     * 获取字典键值对的映射方法
     *
     * @param dictKeyClazzName 字典key类全路径名
     * @param dictValClazzName 字典val类全路径名
     * @return 字典键值对的映射方法
     */
    @SuppressWarnings("rawtypes")
    private Function<String, Map> getDictValMapper(String dictKeyClazzName, String dictValClazzName) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JavaType javaType = objectMapper.getTypeFactory().constructParametricType(Map.class, Class.forName(dictKeyClazzName), Class.forName(dictValClazzName));
            return e -> {
                try {
                    return objectMapper.readValue(e, javaType);
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
     * @param criteria 查询条件
     * @return 查询条件的最大深度
     */
    private int getMaxCriteriaLevel(Criteria criteria) {
        List<Criterion> criterionList = criteria.getCriterionList();
        return criterionList.isEmpty() ? -1 : nodeManagerNameMap.get(criterionList.get(criterionList.size() - 1).getProperty()).property().level();
    }

    /**
     * 查询条件排序
     *
     * @param criteria 查询条件
     */
    private void sortCriteria(Criteria criteria) {
        criteria.getCriterionList().sort(Comparator.comparing(e -> nodeManagerNameMap.get(e.getProperty()).property().level()));
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
        List<Pair<NodeManager, BiConsumer>> propertySetterList = Lists.newArrayList();
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
        HashSet<String> propertySet = Sets.newHashSet(properties);
        List<NodeManager> propertyNodeManagerList = Lists.newArrayList();
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
        // 如果叶子节点不存储数据本身，则只能使用快速删除
        if (!configuration.isLeafNodeAsDataNode() && !configuration.isUseFastErase()) {
            throw new IllegalStateException("Leaf node is not data node, Fast erase support only");
        }
        // 不使用快速删除，可以维护数据总量变量
        if (!configuration.isUseFastErase()) {
            this.size = new LongAdder();
        }
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
     * 查询条件校验
     *
     * @param criteria 查询条件
     */
    private void criteriaCheck(Criteria criteria) {
        List<Criterion> criterionList = criteria.getCriterionList();
        if (criterionList.isEmpty()) {
            return;
        }
        Set<String> propertySet = new HashSet<>();
        for (Criterion criterion : criterionList) {
            if (!nodeManagerNameMap.containsKey(criterion.getProperty())) {
                throw new IllegalArgumentException(propertyNotDefinedMsg(criterion.getProperty()));
            }
            if (!propertySet.add(criterion.getProperty())) {
                throw new IllegalArgumentException("Duplicate property: " + criterion.getProperty() + " in criteria");
            }
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
            return;
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