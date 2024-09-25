package top.chitucao.summerframework.trie;

import static java.util.stream.Collectors.groupingBy;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Test;

import com.google.common.collect.Lists;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.TypeReference;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.json.JSONUtil;
import junit.framework.TestCase;
import top.chitucao.summerframework.trie.configuration.Configuration;
import top.chitucao.summerframework.trie.configuration.property.CustomizedProperty;
import top.chitucao.summerframework.trie.configuration.property.DictKeyType;
import top.chitucao.summerframework.trie.configuration.property.Property;
import top.chitucao.summerframework.trie.configuration.property.SimpleProperty;
import top.chitucao.summerframework.trie.node.NodeType;
import top.chitucao.summerframework.trie.query.*;

/**
 * TrieTest
 *
 * @author chitucao(zhonggang.zhu)
 * @version Id: TrieTest.java, v 0.1 2024-08-09 下午1:31 chitucao Exp $$
 */
public class TrieTest {

    private static final String RESOUCE_FOLDER = "D:\\Develop\\Personal\\category_project\\summer-trie\\src\\test\\resources\\";

    @Test
    public void testSimple() {
        List<TrainSourceDO> dataList = getDataList("train_resource_3000.json");
        Configuration configuration = buildConfiguration1();
        MapTrie<TrainSourceDO> trie = new MapTrie<>(configuration);

        TestCase.assertTrue(configuration.isUseFastErase());
        TestCase.assertEquals(7, trie.getDepth());
        TestCase.assertEquals(0, trie.getSize());

        for (TrainSourceDO data : dataList) {
            trie.insert(data);
        }
        TestCase.assertEquals(3000, trie.getSize());
    }

    @Test
    public void testInsert() {
        List<TrainSourceDO> dataList = getDataList("train_resource_3000.json");
        Configuration configuration = buildConfiguration1();
        MapTrie<TrainSourceDO> trie = new MapTrie<>(configuration);
        TestCase.assertEquals(0, trie.getSize());

        List<TrainSourceDO> dataToInsert = RandomUtil.randomEles(dataList, 10);

        trie.insert(dataToInsert.get(0));
        TestCase.assertEquals(1, trie.getSize());

        trie.insert(dataToInsert.get(0));
        TestCase.assertEquals(1, trie.getSize());

        trie.insert(dataToInsert.get(1));
        TestCase.assertEquals(2, trie.getSize());

        for (TrainSourceDO data : dataToInsert) {
            trie.insert(data);
        }
        TestCase.assertEquals(10, trie.getSize());
    }

    @Test
    public void testEraseByData() {
        List<TrainSourceDO> dataList = getDataList("train_resource_3000.json");
        Configuration configuration = buildConfiguration1();
        MapTrie<TrainSourceDO> trie = new MapTrie<>(configuration);
        for (TrainSourceDO data : dataList) {
            trie.insert(data);
        }
        TestCase.assertEquals(3000, trie.getSize());

        List<TrainSourceDO> dataToErase = RandomUtil.randomEles(dataList, 10);
        for (TrainSourceDO data : dataToErase) {
            trie.erase(data);
        }
        TestCase.assertEquals(2990, trie.getSize());

        trie.insert(dataToErase.get(0));
        TestCase.assertEquals(2991, trie.getSize());
    }

    @Test
    public void testEraseById() {
        List<TrainSourceDO> dataList = getDataList("train_resource_3000.json");
        Configuration configuration = buildConfiguration1();
        MapTrie<TrainSourceDO> trie = new MapTrie<>(configuration);
        for (TrainSourceDO data : dataList) {
            trie.insert(data);
        }

        long id = 143859138L;
        TrainSourceDO eraseData = (TrainSourceDO) trie.dictValues("data", id).iterator().next();
        trie.erase(eraseData);

        TestCase.assertEquals(3000 - 1, trie.getSize());
    }

    @Test
    public void testEraseByCriteria() {
        List<TrainSourceDO> dataList = getDataList("train_resource_3000.json");
        Configuration configuration = buildConfiguration1();
        //        configuration.setUseFastErase(false);
        MapTrie<TrainSourceDO> trie = new MapTrie<>(configuration);
        for (TrainSourceDO data : dataList) {
            trie.insert(data);
        }
        TestCase.assertEquals(3000, trie.getSize());

        //        long eraseCount = dataList.stream().filter(e -> !Arrays.asList(3105, 3109).contains(e.getDepartureCityId())).count();
        //        Criteria criteria = new Criteria().addCriterion(Condition.NOT_IN, Arrays.asList(3105, 3109), "depCityId");
        //        trie.erase(criteria);

        long eraseCount = dataList.stream().filter(e -> e.getDepartureCityId() >= 0 && e.getDepartureCityId() <= 1005).count();
        Criteria criteria = new Criteria().addCriterion(Condition.BETWEEN, 0, 1005, "depCityId");
        trie.erase(criteria);

        TestCase.assertEquals(3000 - eraseCount, trie.getSize());
    }

    @Test
    public void testContains() {
        List<TrainSourceDO> dataList = getDataList("train_resource_3000.json");
        Configuration configuration = buildConfiguration1();
        MapTrie<TrainSourceDO> trie = new MapTrie<>(configuration);

        TrainSourceDO dataToInsert = RandomUtil.randomEle(dataList);

        Criteria criteria = new Criteria().addCriterion(Condition.EQUAL, dataToInsert.getDepartureCityId(), "depCityId");

        TestCase.assertFalse(trie.contains(criteria));
        TestCase.assertFalse(trie.contains(dataToInsert));

        trie.insert(dataToInsert);
        TestCase.assertTrue(trie.contains(criteria));
        TestCase.assertTrue(trie.contains(dataToInsert));
    }

    @Test
    public void testDataSearch() {
        List<TrainSourceDO> dataList = getDataList("train_resource_3000.json");
        Configuration configuration = buildConfiguration1();
        MapTrie<TrainSourceDO> trie = new MapTrie<>(configuration);
        for (TrainSourceDO data : dataList) {
            trie.insert(data);
        }

        List<Integer> queryDepCityList = Lists.newArrayList(144, 145, 146, 900);
        List<TrainSourceDO> dataList1 = dataList.stream().filter(e -> queryDepCityList.contains(e.getDepartureCityId())).sorted(Comparator.comparing(TrainSourceDO::getId))
            .collect(Collectors.toList());

        Criteria criteria = new Criteria();
        criteria.addCriterion(Condition.IN, queryDepCityList, "depCityId");
        List<TrainSourceDO> dataList2 = trie.dataSearch(criteria).stream().sorted(Comparator.comparing(TrainSourceDO::getId)).collect(Collectors.toList());

        TestCase.assertTrue(CollectionUtil.isEqualList(dataList1, dataList2));
    }

    @Test
    public void testPropertySearch() {
        List<TrainSourceDO> dataList = getDataList("train_resource_3000.json");
        Configuration configuration = buildConfiguration1();
        MapTrie<TrainSourceDO> trie = new MapTrie<>(configuration);
        for (TrainSourceDO data : dataList) {
            trie.insert(data);
        }

        List<Integer> queryDepCityList = Lists.newArrayList(144, 145, 146, 900);
        List<Integer> indexList1 = dataList.stream().filter(e -> queryDepCityList.contains(e.getDepartureCityId())).map(TrainSourceDO::getArrivalCityId).distinct().sorted()
            .collect(Collectors.toList());

        Criteria criteria = new Criteria();
        criteria.addCriterion(Condition.IN, queryDepCityList, "depCityId");
        List<Integer> indexList2 = trie.<Integer> propertySearch(criteria, "arrCityId").stream().sorted().collect(Collectors.toList());

        TestCase.assertTrue(CollectionUtil.isEqualList(indexList1, indexList2));
    }

    @Test
    public void testListSearch() {
        List<TrainSourceDO> dataList = getDataList("train_resource_3000.json");
        Configuration configuration = buildConfiguration1();
        MapTrie<TrainSourceDO> trie = new MapTrie<>(configuration);
        for (TrainSourceDO data : dataList) {
            trie.insert(data);
        }

        List<Integer> queryDepCityList = Lists.newArrayList(144, 145, 146, 900);
        List<TrainSourceDO> dataList1 = dataList.stream().filter(e -> queryDepCityList.contains(e.getDepartureCityId())).sorted(Comparator.comparing(TrainSourceDO::getId))
            .collect(Collectors.toList());

        Criteria criteria = new Criteria();
        criteria.addCriterion(Condition.IN, queryDepCityList, "depCityId");

        Aggregations aggregations = new Aggregations();

        ResultBuilder<TrainSourceResult> resultBuilder = new ResultBuilder<>(TrainSourceResult::new);
        resultBuilder.addSetter("id", TrainSourceResult::setId);
        resultBuilder.addSetter("depCityId", TrainSourceResult::setDepCityId);
        resultBuilder.addSetter("arrCityId", TrainSourceResult::setArrCityId);
        resultBuilder.addSetter("trainType", TrainSourceResult::setTrainType);
        resultBuilder.addSetter("seatClass", TrainSourceResult::setSeatClass);

        List<TrainSourceResult> dataList2 = trie.listSearch(criteria, aggregations, resultBuilder);
        CollectionUtil.sort(dataList2, Comparator.comparingLong(TrainSourceResult::getId));

        TestCase.assertEquals(dataList1.size(), dataList2.size());

        for (int i = 0; i < dataList1.size(); i++) {
            TrainSourceDO data1 = dataList1.get(i);
            TrainSourceResult data2 = dataList2.get(i);

            TestCase.assertEquals(data1.getDepartureCityId(), (int) data2.getDepCityId());
            TestCase.assertEquals(data1.getArrivalCityId(), (int) data2.getArrCityId());
            TestCase.assertEquals(data1.getId(), (long) data2.getId());
            TestCase.assertEquals(data1.getTrainType(), data2.getTrainType());
            TestCase.assertEquals(data1.getSeatClass(), data2.getSeatClass());
        }
    }

    @Test
    public void testListSearchAndAgg() {
        List<TrainSourceDO> dataList = getDataList("train_resource_3000.json");
        Configuration configuration = buildConfiguration2();
        MapTrie<TrainSourceDO> trie = new MapTrie<>(configuration);
        for (TrainSourceDO data : dataList) {
            trie.insert(data);
        }
        Map<String, List<TrainSourceDO>> dataMap1 = dataList.stream().collect(groupingBy(e -> e.getDepartureCityId() + "&" + e.getArrivalCityId()));
        HashMap<String, Integer> aggMap1 = new HashMap<>();
        for (Map.Entry<String, List<TrainSourceDO>> entry : dataMap1.entrySet()) {
            aggMap1.put(entry.getKey(), entry.getValue().stream().map(TrainSourceDO::getMinRealPrice).map(Double::intValue).min(Comparator.naturalOrder()).orElse(0));
        }

        ResultBuilder<TrainSourceResultAgg> resultBuilder = new ResultBuilder<>(TrainSourceResultAgg::new);
        resultBuilder.addSetter("price", TrainSourceResultAgg::setMinPrice);
        resultBuilder.addSetter("depCityId", TrainSourceResultAgg::setDepCityId);
        resultBuilder.addSetter("arrCityId", TrainSourceResultAgg::setArrCityId);

        List<TrainSourceResultAgg> dataList1 = trie.listSearch(new Criteria(), new Aggregations().addAggregation(Aggregation.MIN, "price"), resultBuilder);
        HashMap<String, Integer> aggMap2 = new HashMap<>();
        for (TrainSourceResultAgg data : dataList1) {
            aggMap2.put(data.getDepCityId() + "&" + data.getArrCityId(), data.getMinPrice());
        }

        TestCase.assertEquals(aggMap1.size(), aggMap2.size());
        for (Map.Entry<String, Integer> entry : aggMap1.entrySet()) {
            TestCase.assertEquals(entry.getValue(), aggMap2.get(entry.getKey()));
        }
    }

    @Test
    public void testTreeSearch() {
        List<TrainSourceDO> dataList = getDataList("train_resource_3000.json");
        Configuration configuration = buildConfiguration1();
        MapTrie<TrainSourceDO> trie = new MapTrie<>(configuration);
        for (TrainSourceDO data : dataList) {
            trie.insert(data);
        }

        List<Integer> queryDepCityList = Lists.newArrayList(144, 145, 146, 900);
        List<TrainSourceDO> dataList1 = dataList.stream().filter(e -> queryDepCityList.contains(e.getDepartureCityId())).sorted(Comparator.comparing(TrainSourceDO::getId))
            .collect(Collectors.toList());
        Map<Integer, Map<Integer, Map<Long, List<TrainSourceDO>>>> depCityMap1 = dataList1.stream()
            .collect(groupingBy(TrainSourceDO::getDepartureCityId, groupingBy(TrainSourceDO::getArrivalCityId, groupingBy(TrainSourceDO::getId))));

        Map<Integer, Map<Integer, List<Long>>> depCityMap0 = dataList1.stream()
            .collect(groupingBy(TrainSourceDO::getDepartureCityId, Collectors.toMap(TrainSourceDO::getArrivalCityId, e -> Lists.newArrayList(e.getId()), (a, b) -> {
                a.addAll(b);
                return a;
            })));

        Criteria criteria = new Criteria();
        criteria.addCriterion(Condition.IN, queryDepCityList, "depCityId");

        Aggregations aggregations = new Aggregations();

        Object result = trie.treeSearch(criteria, aggregations, "depCityId", "arrCityId", "id");
        System.out.println(JSONUtil.toJsonStr(result));

        Map<Integer, Object> depCityMap2 = (Map) result;

        TestCase.assertTrue(
            CollectionUtil.isEqualList(depCityMap1.keySet().stream().sorted().collect(Collectors.toList()), depCityMap2.keySet().stream().sorted().collect(Collectors.toList())));

        String str1 = JSONUtil.toJsonStr(depCityMap0);
        String str2 = JSONUtil.toJsonStr(result);

        for (Map.Entry<Integer, Map<Integer, Map<Long, List<TrainSourceDO>>>> entry : depCityMap1.entrySet()) {
            Integer depCityId = entry.getKey();
            Map<Integer, Map<Long, List<TrainSourceDO>>> arrCityMap1 = entry.getValue();
            Map<Integer, Object> arrCityMap2 = (Map) depCityMap2.get(depCityId);
            TestCase.assertTrue(CollectionUtil.isEqualList(arrCityMap1.keySet().stream().sorted().collect(Collectors.toList()),
                arrCityMap2.keySet().stream().sorted().collect(Collectors.toList())));
            for (Map.Entry<Integer, Map<Long, List<TrainSourceDO>>> entry1 : arrCityMap1.entrySet()) {
                Integer arrCityId = entry1.getKey();
                Map<Long, List<TrainSourceDO>> idMap1 = entry1.getValue();
                List<Integer> idList2 = (List<Integer>) arrCityMap2.get(arrCityId);
                TestCase.assertTrue(
                    CollectionUtil.isEqualList(idMap1.keySet().stream().sorted().collect(Collectors.toList()), idList2.stream().sorted().collect(Collectors.toList())));
            }
        }
    }

    @Test
    public void testTreeSearchOneLevel() {
        List<TrainSourceDO> dataList = getDataList("train_resource_3000.json");
        Configuration configuration = buildConfiguration1();
        MapTrie<TrainSourceDO> trie = new MapTrie<>(configuration);
        for (TrainSourceDO data : dataList) {
            trie.insert(data);
        }

        List<Integer> queryDepCityList = Lists.newArrayList(144, 145, 146, 900);
        List<Integer> dataList1 = dataList.stream().filter(e -> !queryDepCityList.contains(e.getDepartureCityId())).map(TrainSourceDO::getArrivalDistrictId).distinct().sorted()
            .collect(Collectors.toList());

        Criteria criteria = new Criteria();
        criteria.addCriterion(Condition.NOT_IN, queryDepCityList, "depCityId");

        Aggregations aggregations = new Aggregations();
        List<Integer> dataList2 = ((List<Integer>) trie.treeSearch(criteria, aggregations, "arrDistrictId")).stream().sorted().collect(Collectors.toList());
        TestCase.assertTrue(CollectionUtil.isEqualList(dataList1, dataList2));
    }

    @Test
    public void testDictValues() {
        List<TrainSourceDO> dataList = getDataList("train_resource_3000.json");
        Configuration configuration = buildConfiguration1();
        MapTrie<TrainSourceDO> trie = new MapTrie<>(configuration);
        for (TrainSourceDO data : dataList) {
            trie.insert(data);
        }

        List<Integer> dataList1 = dataList.stream().map(TrainSourceDO::getDepartureCityId).distinct().sorted().collect(Collectors.toList());
        List<Object> dataList2 = trie.dictValues("depCityId").stream().sorted().collect(Collectors.toList());
        TestCase.assertTrue(CollectionUtil.isEqualList(dataList1, dataList2));

        List<Integer> dataList3 = dataList.stream().map(TrainSourceDO::getArrivalDistrictId).distinct().sorted().collect(Collectors.toList());
        List<Object> dataList4 = trie.dictValues("arrDistrictId").stream().sorted().collect(Collectors.toList());
        TestCase.assertTrue(CollectionUtil.isEqualList(dataList3, dataList4));
    }

    @Test
    public void testSerializeAndDeserialize() {
        List<TrainSourceDO> dataList = getDataList("train_resource_3000.json");
        Configuration configuration1 = buildConfiguration1();
        MapTrie<TrainSourceDO> trie1 = new MapTrie<>(configuration1);
        for (TrainSourceDO data : dataList) {
            trie1.insert(data);
        }
        byte[] bytes = trie1.serialize();
        Configuration configuration2 = buildConfiguration1();
        MapTrie<TrainSourceDO> trie2 = new MapTrie<>(configuration2);
        trie2.deserialize(bytes);
        TestCase.assertEquals(trie1.getSize(), trie2.getSize());

        Map<String, Property> propertyMap1 = configuration1.getProperties().stream().collect(Collectors.toMap(Property::name, Function.identity()));
        Map<String, Property> propertyMap2 = configuration2.getProperties().stream().collect(Collectors.toMap(Property::name, Function.identity()));

        for (String propertyName : configuration1.getProperties().stream().map(Property::name).collect(Collectors.toList())) {
            Set<Object> set1 = trie1.dictValues(propertyName);
            Set<Object> set2 = trie2.dictValues(propertyName);
            TestCase.assertTrue(set1.containsAll(set2) && set2.containsAll(set1));
            Property property = propertyMap1.get(propertyName);
            if (property instanceof SimpleProperty) {
                TestCase.assertEquals(((SimpleProperty) propertyMap1.get(propertyName)).getDictKeyAdder().getId(),
                    ((SimpleProperty) propertyMap2.get(propertyName)).getDictKeyAdder().getId());
            }
        }
    }

    @Test
    public void testDumpAndImport() {
        List<TrainSourceDO> dataList = getDataList("train_resource_3000.json");
        Configuration configuration = buildConfiguration1();
        MapTrie<TrainSourceDO> trie1 = new MapTrie<>(configuration);
        for (TrainSourceDO data : dataList) {
            trie1.insert(data);
        }
        byte[] bytes1 = trie1.serialize();

        File dumpFile = new File(RESOUCE_FOLDER + "train_resource_dump.dat");
        if (dumpFile.exists()) {
            dumpFile.delete();
        }
        FileUtil.writeBytes(bytes1, dumpFile);
        byte[] bytes2 = FileUtil.readBytes(dumpFile);
        Configuration configuration2 = buildConfiguration1();
        MapTrie<TrainSourceDO> trie2 = new MapTrie<>(configuration2);
        trie2.deserialize(bytes2);

        TestCase.assertEquals(trie1.getSize(), trie2.getSize());

        for (String propertyName : configuration.getProperties().stream().map(Property::name).collect(Collectors.toList())) {
            Set<Object> set1 = trie1.dictValues(propertyName);
            Set<Object> set2 = trie2.dictValues(propertyName);
            TestCase.assertTrue(set1.containsAll(set2) && set2.containsAll(set1));
        }
    }

    @Test
    public void testSerialAll() {
        String resourceFileName = "train_resource_3000.json";
        List<TrainSourceDO> dataList = getDataList(resourceFileName);
        Date now = new Date();
        for (TrainSourceDO trainSourceDO : dataList) {
            if (Objects.isNull(trainSourceDO.getCreateDate())) {
                trainSourceDO.setCreateDate(now);
            }
        }
        MapTrie<TrainSourceDO> trie1 = new MapTrie<>(buildConfiguration3());
        for (TrainSourceDO data : dataList) {
            trie1.insert(data);
        }
        File dumpFile = new File(RESOUCE_FOLDER + "train_resource_dump.dat");
        if (dumpFile.exists()) {
            dumpFile.delete();
        }
        FileUtil.writeBytes(trie1.serialize(), dumpFile);

        MapTrie<TrainSourceDO> trie2 = new MapTrie<>(buildConfiguration3());
        trie2.deserialize(FileUtil.readBytes(dumpFile));
        List<TrainSourceDO> dataList2 = trie2.<TrainSourceDO> listSearch(new Criteria(), new Aggregations(), buildResultBuilder());

        dataList.sort(Comparator.comparing(TrainSourceDO::getId));
        dataList2.sort(Comparator.comparing(TrainSourceDO::getId));
        TestCase.assertTrue(CollectionUtil.isEqualList(dataList, dataList2));

        long sourceFileSize = FileUtil.size(new File(RESOUCE_FOLDER + resourceFileName));
        long dumpFileSize = FileUtil.size(dumpFile);
        TestCase.assertTrue(dumpFileSize < sourceFileSize);

        Map<String, Integer> dictSizes1 = trie1.dictSizes();
        Map<String, Integer> dictSizes2 = trie2.dictSizes();

        TestCase.assertTrue(dictSizes1.values().containsAll(dictSizes2.values()) && dictSizes2.values().containsAll(dictSizes1.values()));
    }

    @Test
    public void testZipWithSortProperty() {
        // json
        String dataSource = "train_resource_3000.json";
        List<TrainSourceDO> dataList = getDataList(dataSource);
        System.out.println("数据量：" + dataList.size());

        long start, end;

        Date now = new Date();

        for (TrainSourceDO trainSourceDO : dataList) {
            if (Objects.isNull(trainSourceDO.getCreateDate())) {
                trainSourceDO.setCreateDate(now);
            }
        }

        Configuration configuration1 = buildConfiguration3();
        MapTrie<TrainSourceDO> trie1 = new MapTrie<>(configuration1);
        start = System.currentTimeMillis();
        for (TrainSourceDO data : dataList) {
            trie1.insert(data);
        }
        end = System.currentTimeMillis();
        System.out.println("未排序构建耗时" + (end - start) + "ms");

        Map<String, Integer> dictSizes = trie1.dictSizes();

        Configuration configuration2 = buildConfiguration3();
        configuration2.sortProperties(Comparator.comparing(dictSizes::get));

        CollectionUtil.sort(configuration2.getProperties(), Comparator.comparing(e -> dictSizes.get(e.name())));
        MapTrie<TrainSourceDO> trie2 = new MapTrie<>(configuration2);
        start = System.currentTimeMillis();
        for (TrainSourceDO data : dataList) {
            trie2.insert(data);
        }
        end = System.currentTimeMillis();
        System.out.println("排序后构建耗时" + (end - start) + "ms");

        ////
        File jsonfile = FileUtil.newFile(RESOUCE_FOLDER + "train_resource_origin.json");
        if (jsonfile.exists()) {
            jsonfile.delete();
        }
        start = System.currentTimeMillis();
        FileUtil.writeBytes(JSONUtil.toJsonStr(dataList).getBytes(), jsonfile);
        end = System.currentTimeMillis();
        System.out.println("原始列表json大小：" + jsonfile.length() + " 序列化耗时：" + (end - start) + "ms");

        // tree1
        Object treeResult1 = trie1.treeSearch(new Criteria(), new Aggregations(), configuration1.getProperties().stream().map(Property::name).toArray(String[]::new));
        File tree1 = new File(RESOUCE_FOLDER + "train_resource_origin_tree.json");
        if (tree1.exists()) {
            tree1.delete();
        }
        start = System.currentTimeMillis();
        FileUtil.writeBytes(JSONUtil.toJsonStr(treeResult1).getBytes(), tree1);
        end = System.currentTimeMillis();
        System.out.println("未排序树json大小：" + tree1.length() + " 序列化耗时：" + (end - start) + "ms");

        // protobuf1
        start = System.currentTimeMillis();
        byte[] bytes1 = trie1.serialize();
        File protobuf1 = new File(RESOUCE_FOLDER + "train_resource_origin_protobuf.dat");
        if (protobuf1.exists()) {
            protobuf1.delete();
        }
        FileUtil.writeBytes(bytes1, protobuf1);
        end = System.currentTimeMillis();
        System.out.println("未排序protobuf序列化大小：" + protobuf1.length() + " 序列化耗时：" + (end - start) + "ms");

        // tree2
        Object treeResult2 = trie2.treeSearch(new Criteria(), new Aggregations(), configuration2.getProperties().stream().map(Property::name).toArray(String[]::new));
        File tree2 = new File(RESOUCE_FOLDER + "train_resource_sorted_tree.json");
        if (tree2.exists()) {
            tree2.delete();
        }
        start = System.currentTimeMillis();
        FileUtil.writeBytes(JSONUtil.toJsonStr(treeResult2).getBytes(), tree2);
        end = System.currentTimeMillis();
        System.out.println("排序后树json大小：" + tree2.length() + " 序列化耗时：" + (end - start) + "ms");

        // protobuf2
        start = System.currentTimeMillis();
        byte[] bytes2 = trie2.serialize();
        File protobuf2 = new File(RESOUCE_FOLDER + "train_resource_sorted_protobuf.dat");
        if (protobuf2.exists()) {
            protobuf2.delete();
        }
        FileUtil.writeBytes(bytes2, protobuf2);
        end = System.currentTimeMillis();
        System.out.println("排序后protobuf序列化大小：" + protobuf2.length() + " 序列化耗时：" + (end - start) + "ms");

        TestCase.assertTrue(bytes2.length < bytes1.length);
        TestCase.assertTrue(FileUtil.size(protobuf2) < FileUtil.size(protobuf1));
        TestCase.assertTrue(FileUtil.size(tree2) < FileUtil.size(tree1));
        TestCase.assertTrue(FileUtil.size(protobuf1) < FileUtil.size(tree1) && FileUtil.size(tree1) < FileUtil.size(jsonfile));
        TestCase.assertTrue(FileUtil.size(protobuf2) < FileUtil.size(tree2) && FileUtil.size(tree2) < FileUtil.size(jsonfile));

        List<TrainSourceDO> dataList2 = trie2.<TrainSourceDO> listSearch(new Criteria(), new Aggregations(), buildResultBuilder());

        dataList.sort(Comparator.comparing(TrainSourceDO::getId));
        dataList2.sort(Comparator.comparing(TrainSourceDO::getId));
        TestCase.assertTrue(CollectionUtil.isEqualList(dataList, dataList2));
    }

    private List<TrainSourceDO> getDataList(String dataSource) {
        File file = FileUtil.newFile(RESOUCE_FOLDER + dataSource);
        String json = FileUtil.readString(file, StandardCharsets.UTF_8);
        TypeReference<List<TrainSourceDO>> typeReference = new TypeReference<List<TrainSourceDO>>() {
        };
        return JSONUtil.toBean(json, typeReference, true);
    }

    private ResultBuilder buildResultBuilder() {
        ResultBuilder<TrainSourceDO> resultBuilder = new ResultBuilder<>(TrainSourceDO::new);
        Field[] fields = ReflectUtil.getFields(TrainSourceDO.class);
        for (Field field : fields) {
            resultBuilder.addSetter(field.getName(), (t, r) -> ReflectUtil.setFieldValue(t, field, r));
        }
        return resultBuilder;
    }

    private Configuration buildConfiguration3() {
        Configuration configuration = new Configuration();
        Field[] fields = ReflectUtil.getFields(TrainSourceDO.class);
        for (Field field : fields) {
            if (Number.class.isAssignableFrom(field.getType())) {
                CustomizedProperty customizedProperty = new CustomizedProperty<>(field.getName());
                customizedProperty.setPropertyMapper(e -> ReflectUtil.getFieldValue(e, field));
                customizedProperty.setDictKeyMapper(r -> r);
                configuration.addProperty(customizedProperty);
            } else {
                SimpleProperty simpleProperty = new SimpleProperty<>(field.getName());
                simpleProperty.setPropertyMapper(e -> ReflectUtil.getFieldValue(e, field));
                configuration.addProperty(simpleProperty);
            }
        }
        return configuration;
    }

    private Configuration buildConfiguration2() {
        Configuration configuration = new Configuration();
        // 出发城市
        CustomizedProperty<TrainSourceDO, Integer> depCityIdProperty = new CustomizedProperty<>("depCityId", NodeType.TREE_MAP);
        depCityIdProperty.setPropertyMapper(TrainSourceDO::getDepartureCityId);
        depCityIdProperty.setDictKeyMapper(r -> r);
        configuration.addProperty(depCityIdProperty);

        // 抵达城市
        CustomizedProperty<TrainSourceDO, Integer> arrCityIdProperty = new CustomizedProperty<>("arrCityId", NodeType.TREE_MAP);
        arrCityIdProperty.setPropertyMapper(TrainSourceDO::getArrivalCityId);
        arrCityIdProperty.setDictKeyMapper(r -> r);
        configuration.addProperty(arrCityIdProperty);

        // 价格
        CustomizedProperty<TrainSourceDO, Integer> arrDistrictIdProperty = new CustomizedProperty<>("price", NodeType.TREE_MAP);
        arrDistrictIdProperty.setPropertyMapper(t -> ((Double) t.getMinRealPrice()).intValue());
        arrDistrictIdProperty.setDictKeyMapper(r -> r);
        configuration.addProperty(arrDistrictIdProperty);

        return configuration;
    }

    private Configuration buildConfiguration1() {
        Configuration configuration = new Configuration();
        // 出发城市
        CustomizedProperty<TrainSourceDO, Integer> depCityIdProperty = new CustomizedProperty<>("depCityId", NodeType.TREE_MAP);
        depCityIdProperty.setPropertyMapper(TrainSourceDO::getDepartureCityId);
        depCityIdProperty.setDictKeyMapper(r -> r);
        configuration.addProperty(depCityIdProperty);

        // 出发地区
        CustomizedProperty<TrainSourceDO, Integer> depDistrictIdProperty = new CustomizedProperty<>("depDistrictId", NodeType.TREE_MAP);
        depDistrictIdProperty.setPropertyMapper(TrainSourceDO::getDepartureDistrictId);
        depDistrictIdProperty.setDictKeyMapper(r -> r);
        configuration.addProperty(depDistrictIdProperty);

        // 抵达城市
        CustomizedProperty<TrainSourceDO, Integer> arrCityIdProperty = new CustomizedProperty<>("arrCityId", NodeType.TREE_MAP);
        arrCityIdProperty.setPropertyMapper(TrainSourceDO::getArrivalCityId);
        arrCityIdProperty.setDictKeyMapper(r -> r);
        configuration.addProperty(arrCityIdProperty);

        // 抵达地区
        CustomizedProperty<TrainSourceDO, Integer> arrDistrictIdProperty = new CustomizedProperty<>("arrDistrictId", NodeType.TREE_MAP);
        arrDistrictIdProperty.setPropertyMapper(TrainSourceDO::getArrivalDistrictId);
        arrDistrictIdProperty.setDictKeyMapper(r -> r);
        configuration.addProperty(arrDistrictIdProperty);

        // 车次类型
        SimpleProperty<TrainSourceDO, String> trainTypeProperty = new SimpleProperty<>("trainType", DictKeyType.BYTE);
        trainTypeProperty.setPropertyMapper(TrainSourceDO::getTrainType);
        configuration.addProperty(trainTypeProperty);

        // 坐席类型
        SimpleProperty<TrainSourceDO, String> seatClassProperty = new SimpleProperty<>("seatClass", DictKeyType.BYTE);
        seatClassProperty.setPropertyMapper(TrainSourceDO::getSeatClass);
        configuration.addProperty(seatClassProperty);

        // id
        CustomizedProperty<TrainSourceDO, Long> idProperty = new CustomizedProperty<>("id", NodeType.TREE_MAP);
        idProperty.setPropertyMapper(TrainSourceDO::getId);
        idProperty.setDictKeyMapper(r -> r);
        configuration.addProperty(idProperty);

        // 数据
        CustomizedProperty<TrainSourceDO, TrainSourceDO> dataProperty = new CustomizedProperty<>("data", NodeType.TREE_MAP);
        dataProperty.setPropertyMapper(Function.identity());
        dataProperty.setDictKeyMapper(TrainSourceDO::getId);
        configuration.addProperty(dataProperty);

        return configuration;
    }
}