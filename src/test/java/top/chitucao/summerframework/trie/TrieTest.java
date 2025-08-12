package top.chitucao.summerframework.trie;

import static java.util.stream.Collectors.groupingBy;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
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
import top.chitucao.summerframework.trie.flight.FlightResourceDO;
import top.chitucao.summerframework.trie.flight.FlightTrieIndexNames;
import top.chitucao.summerframework.trie.node.NodeType;
import top.chitucao.summerframework.trie.query.Aggregation;
import top.chitucao.summerframework.trie.query.Aggregations;
import top.chitucao.summerframework.trie.query.Criteria;
import top.chitucao.summerframework.trie.query.ResultBuilder;
import top.chitucao.summerframework.trie.train.TrainSourceDO;
import top.chitucao.summerframework.trie.train.TrainSourceResult;
import top.chitucao.summerframework.trie.train.TrainSourceResultAgg;
import top.chitucao.summerframework.trie.train.TrainTrieIndexNames;
import top.chitucao.summerframework.trie.utils.Pair;

/**
 * TrieTest
 *
 * @author chitucao
 */
public class TrieTest {

    // 改成你自己的resouce路径
    private static final String RESOUCE_FOLDER = "D:\\Develop\\Personal\\category_project\\summer-trie\\src\\test\\resources\\";

    @Test
    public void testSimple() {
        List<TrainSourceDO> dataList = getDataList("train_resource_3000.json");
        Configuration configuration = buildConfiguration1();
        MapTrie<TrainSourceDO> trie = new MapTrie<>(configuration);

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

        //        long eraseCount = dataList.stream().filter(e -> Arrays.asList(3105, 3109).contains(e.getDepartureCityId())).count();
        //        Criteria criteria = new Criteria().addCriterion(Condition.IN, Arrays.asList(3105, 3109), "depCityId");
        //        int queryCount = trie.dataSearch(criteria).size();
        //        int count = trie.erase(criteria);

        long eraseCount = dataList.stream().filter(e -> e.getDepartureCityId() >= 0 && e.getDepartureCityId() <= 1005 && Objects.equals(e.getTrainType(), "KS")).count();
        Criteria criteria = Criteria.where("depCityId").between(0, 1005).and("trainType").eq("KS");
        int queryCount = trie.dataSearch(criteria).size();
        int count = trie.erase(criteria);

        if (!configuration.isUseFastErase()) {
            TestCase.assertEquals(queryCount, count);
        }

        TestCase.assertEquals(3000 - eraseCount, trie.getSize());
    }

    @Test
    public void testContains() {
        List<TrainSourceDO> dataList = getDataList("train_resource_3000.json");
        Configuration configuration = buildConfiguration1();
        MapTrie<TrainSourceDO> trie = new MapTrie<>(configuration);

        TrainSourceDO dataToInsert = RandomUtil.randomEle(dataList);

        Criteria criteria = Criteria.where("depCityId").eq(dataToInsert.getDepartureCityId());

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

        List<Integer> queryDepCityList = Arrays.asList(144, 145, 146, 900);
        List<TrainSourceDO> dataList1 = dataList.stream().filter(e -> queryDepCityList.contains(e.getDepartureCityId())).sorted(Comparator.comparing(TrainSourceDO::getId))
            .collect(Collectors.toList());

        Criteria criteria = Criteria.where("depCityId").in(queryDepCityList);

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

        List<Integer> queryDepCityList = Arrays.asList(144, 145, 146, 900);
        List<Integer> indexList1 = dataList.stream().filter(e -> queryDepCityList.contains(e.getDepartureCityId())).map(TrainSourceDO::getArrivalCityId).distinct().sorted()
            .collect(Collectors.toList());

        Criteria criteria = Criteria.where("depCityId").in(queryDepCityList);

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

        List<Integer> queryDepCityList = Arrays.asList(144, 145, 146, 900);
        List<TrainSourceDO> dataList1 = dataList.stream().filter(e -> queryDepCityList.contains(e.getDepartureCityId())).sorted(Comparator.comparing(TrainSourceDO::getId))
            .collect(Collectors.toList());

        Criteria criteria = Criteria.where("depCityId").in(queryDepCityList);

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

        List<TrainSourceResultAgg> dataList1 = trie.listSearch(null, new Aggregations().addAggregation(Aggregation.MIN, "price"), resultBuilder);
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

        List<Integer> queryDepCityList = Arrays.asList(144, 145, 146, 900);
        List<TrainSourceDO> dataList1 = dataList.stream().filter(e -> queryDepCityList.contains(e.getDepartureCityId())).sorted(Comparator.comparing(TrainSourceDO::getId))
            .collect(Collectors.toList());
        Map<Integer, Map<Integer, Map<Long, List<TrainSourceDO>>>> depCityMap1 = dataList1.stream()
            .collect(groupingBy(TrainSourceDO::getDepartureCityId, groupingBy(TrainSourceDO::getArrivalCityId, groupingBy(TrainSourceDO::getId))));

        Map<Integer, Map<Integer, List<Long>>> depCityMap0 = dataList1.stream().collect(
            groupingBy(TrainSourceDO::getDepartureCityId, Collectors.toMap(TrainSourceDO::getArrivalCityId, e -> new ArrayList<>(Collections.singletonList(e.getId())), (a, b) -> {
                a.addAll(b);
                return a;
            })));

        Criteria criteria = Criteria.where("depCityId").in(queryDepCityList);

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
                TestCase
                    .assertTrue(CollectionUtil.isEqualList(idMap1.keySet().stream().sorted().collect(Collectors.toList()), idList2.stream().sorted().collect(Collectors.toList())));
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

        List<Integer> queryDepCityList = Arrays.asList(144, 145, 146, 900);
        List<Integer> dataList1 = dataList.stream().filter(e -> !queryDepCityList.contains(e.getDepartureCityId())).map(TrainSourceDO::getArrivalDistrictId).distinct().sorted()
            .collect(Collectors.toList());

        Criteria criteria = Criteria.where("arrCityId").nin(queryDepCityList);

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
        MapTrie<TrainSourceDO> trie1 = new MapTrie<>(buildConfiguration3(TrainSourceDO.class));
        for (TrainSourceDO data : dataList) {
            trie1.insert(data);
        }
        File dumpFile = new File(RESOUCE_FOLDER + "train_resource_dump.dat");
        if (dumpFile.exists()) {
            dumpFile.delete();
        }
        FileUtil.writeBytes(trie1.serialize(), dumpFile);

        MapTrie<TrainSourceDO> trie2 = new MapTrie<>(buildConfiguration3(TrainSourceDO.class));
        trie2.deserialize(FileUtil.readBytes(dumpFile));
        List<TrainSourceDO> dataList2 = trie2.<TrainSourceDO> listSearch(null, new Aggregations(), buildResultBuilder());

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
    public void testMultiConditionSearch() {
        List<TrainSourceDO> dataList = getDataList("train_resource_3000.json");
        Configuration configuration = buildConfiguration1();
        MapTrie<TrainSourceDO> trie = new MapTrie<>(configuration);
        for (TrainSourceDO data : dataList) {
            trie.insert(data);
        }
        TestCase.assertEquals(3000, trie.getSize());

        Set<Integer> notInDepartureCityIdList = IntStream.range(0, 100).boxed().collect(Collectors.toSet());

        List<String> inDepartureStationCodeList = Arrays.asList("SDG", "SAH", "RZH", "RYH");

        List<TrainSourceDO> result1 = dataList.stream() //
            .filter(e -> e.getDepartureCityId() >= 0 && e.getDepartureCityId() <= 1005) //
            .filter(e -> e.getDepartureCityId() != 391)//
            .filter(e -> !notInDepartureCityIdList.contains(e.getDepartureCityId())) //
            .filter(e -> e.getTrainType().equals("KS")) //
            .filter(e -> Double.valueOf(e.getMinRealPrice()).intValue() <= 200) //
            .filter(e -> Double.valueOf(e.getMinRealPrice()).intValue() > 100) //
            .filter(e -> inDepartureStationCodeList.contains(e.getDepartureStationCode())) //
            .collect(Collectors.toList());

        Criteria criteria = Criteria.where("depCityId").between(0, 1005) //
            .and("depCityId").nin(notInDepartureCityIdList) //
            .and("depCityId").ne(391)//
            .and("trainType").eq("KS") //
            .and("minRealPrice").lte(200) //
            .and("minRealPrice").gt(100) //
            .and("departureStationCode").in(inDepartureStationCodeList); //
        List<TrainSourceDO> result2 = trie.dataSearch(criteria);

        TestCase.assertEquals(result1.size(), result2.size());
    }

    @Test
    public void testTrainSerial() {
        long start, end;
        Supplier<Configuration> buildConfigurationSupplier = this::buildTrainQueryTrieConfiguration;
        //        Supplier<Configuration> buildConfigurationSupplier = () -> buildConfiguration3(TrainSourceDO.class);

        String dataSource = "train_resource_30w.json";
        if (!FileUtil.exist(RESOUCE_FOLDER + dataSource)) {
            System.out.println("train_resource_30w.json not found！");
            return;
        }
        // 读取json数据
        triggerGc();
        printMemoryUse();
        start = System.currentTimeMillis();
        List<TrainSourceDO> dataList = getDataList(dataSource);
        end = System.currentTimeMillis();
        triggerGc();
        printMemoryUse();
        System.out.println("数据量：" + dataList.size());
        System.out.println("读取json数据耗时：" + (end - start) + "ms");

        // 原始数据 json序列化
        File jsonfile = FileUtil.newFile(RESOUCE_FOLDER + "train_resource_origin.json");
        if (jsonfile.exists()) {
            jsonfile.delete();
        }
        start = System.currentTimeMillis();
        FileUtil.writeBytes(JSONUtil.toJsonStr(dataList).getBytes(), jsonfile);
        end = System.currentTimeMillis();
        System.out.println("json序列化大小：" + jsonfile.length() / 1024 / 1024 + "MB 耗时：" + (end - start) + "ms");

        // 原始数据 json反序列化
        start = System.currentTimeMillis();
        List<TrainSourceDO> dataList2 = getDataList("train_resource_origin.json");
        end = System.currentTimeMillis();
        System.out.println("json反序列化耗时：" + (end - start) + "ms");

        // trie 从数据构建
        Configuration configuration = buildConfigurationSupplier.get();
        MapTrie<TrainSourceDO> trie1 = new MapTrie<>(configuration);
        triggerGc();
        printMemoryUse();
        start = System.currentTimeMillis();
        for (TrainSourceDO data : dataList2) {
            trie1.insert(data);
        }
        end = System.currentTimeMillis();
        triggerGc();
        printMemoryUse();
        System.out.println("trie索引字段数量：" + configuration.getProperties().size() + " 构建耗时：" + (end - start) + "ms");

        // trie protobuf序列化
        start = System.currentTimeMillis();
        byte[] bytes1 = trie1.serialize();
        File protobufFile = new File(RESOUCE_FOLDER + "train_resource_origin_protobuf.dat");
        if (protobufFile.exists()) {
            protobufFile.delete();
        }
        FileUtil.writeBytes(bytes1, protobufFile);
        end = System.currentTimeMillis();
        System.out.println("protobuf序列化大小：" + protobufFile.length() / 1024 / 1024 + "MB 耗时：" + (end - start) + "ms");

        // trie protobuf反序列化
        start = System.currentTimeMillis();
        MapTrie<TrainSourceDO> trie2 = new MapTrie<>(buildConfigurationSupplier.get());
        trie2.deserialize(FileUtil.readBytes(protobufFile));
        end = System.currentTimeMillis();
        System.out.println("protobuf反序列化耗时：" + (end - start) + "ms");
    }

    @Test
    public void testFlightSerial() {
        long start, end;

        Supplier<Configuration> buildConfigurationSupplier = this::buildFlightQueryTrieConfiguration;
        //        Supplier<Configuration> buildConfigurationSupplier = () -> buildConfiguration3(FlightResourceDO.class);

        String dataSource = "flight_resource_60w.json";
        if (!FileUtil.exist(RESOUCE_FOLDER + dataSource)) {
            System.out.println("flight_resource_60w.json not found！");
            return;
        }

        // 读取json数据
        triggerGc();
        printMemoryUse();
        start = System.currentTimeMillis();
        List<FlightResourceDO> dataList = getFlightDataList(dataSource);
        end = System.currentTimeMillis();
        triggerGc();
        printMemoryUse();
        System.out.println("数据量：" + dataList.size());
        System.out.println("读取json数据耗时：" + (end - start) + "ms");

        // 原始数据 json序列化
        File jsonfile = FileUtil.newFile(RESOUCE_FOLDER + "flight_resource_origin.json");
        if (jsonfile.exists()) {
            jsonfile.delete();
        }
        start = System.currentTimeMillis();
        FileUtil.writeBytes(JSONUtil.toJsonStr(dataList).getBytes(), jsonfile);
        end = System.currentTimeMillis();
        System.out.println("json序列化大小：" + jsonfile.length() / 1024 / 1024 + "MB 耗时：" + (end - start) + "ms");

        // 原始数据 json反序列化
        start = System.currentTimeMillis();
        List<FlightResourceDO> dataList2 = getFlightDataList("flight_resource_origin.json");
        end = System.currentTimeMillis();
        System.out.println("json反序列化耗时：" + (end - start) + "ms");

        // trie 从数据构建
        Configuration configuration = buildConfigurationSupplier.get();
        MapTrie<FlightResourceDO> trie1 = new MapTrie<>(configuration);
        triggerGc();
        printMemoryUse();
        start = System.currentTimeMillis();
        for (FlightResourceDO data : dataList2) {
            trie1.insert(data);
        }
        end = System.currentTimeMillis();
        triggerGc();
        printMemoryUse();
        System.out.println("trie索引字段数量：" + configuration.getProperties().size() + " 构建耗时：" + (end - start) + "ms");

        // trie protobuf序列化

        start = System.currentTimeMillis();
        byte[] bytes1 = trie1.serialize();
        File protobufFile = new File(RESOUCE_FOLDER + "flight_resource_origin_protobuf.dat");
        if (protobufFile.exists()) {
            protobufFile.delete();
        }
        FileUtil.writeBytes(bytes1, protobufFile);
        end = System.currentTimeMillis();
        System.out.println("protobuf序列化大小：" + protobufFile.length() / 1024 / 1024 + "MB 耗时：" + (end - start) + "ms");

        // trie protobuf反序列化
        start = System.currentTimeMillis();
        MapTrie<FlightResourceDO> trie2 = new MapTrie<>(buildConfigurationSupplier.get());
        trie2.deserialize(FileUtil.readBytes(protobufFile));
        end = System.currentTimeMillis();
        System.out.println("protobuf反序列化耗时：" + (end - start) + "ms");
    }

    @Test
    public void testTrainDeserializeMemoryUse() {
        long start, end;
        Supplier<Configuration> buildConfigurationSupplier = this::buildTrainQueryTrieConfiguration;

        File protobufFile = new File(RESOUCE_FOLDER + "train_resource_origin_protobuf.dat");
        if (!protobufFile.exists()) {
            return;
        }

        triggerGc();
        printMemoryUse();

        start = System.currentTimeMillis();
        Configuration configuration = buildConfigurationSupplier.get();
        MapTrie<TrainSourceDO> trie = new MapTrie<>(configuration);
        trie.deserialize(FileUtil.readBytes(protobufFile));
        end = System.currentTimeMillis();

        triggerGc();
        printMemoryUse();

        // 清空叶子节点的数据
        //        clearDictData(configuration.getLastProperty().dict());
        triggerGc();
        printMemoryUse();

        System.out.println("数据量：" + trie.getSize() + " protobuf反序列化耗时：" + (end - start) + "ms");
    }

    @Test
    public void testFlightDeserializeMemoryUse() {
        long start, end;
        Supplier<Configuration> buildConfigurationSupplier = this::buildFlightQueryTrieConfiguration;

        File protobufFile = new File(RESOUCE_FOLDER + "flight_resource_origin_protobuf.dat");
        if (!protobufFile.exists()) {
            return;
        }

        triggerGc();
        printMemoryUse();

        start = System.currentTimeMillis();
        Configuration configuration = buildConfigurationSupplier.get();
        MapTrie<FlightResourceDO> trie = new MapTrie<>(configuration);
        trie.deserialize(FileUtil.readBytes(protobufFile));
        end = System.currentTimeMillis();

        triggerGc();
        printMemoryUse();

        // 清空叶子节点的数据
        //        clearDictData(configuration.getLastProperty().dict());
        triggerGc();
        printMemoryUse();

        System.out.println("数据量：" + trie.getSize() + " protobuf反序列化耗时：" + (end - start) + "ms");
    }

    //    @Test
    public void testSplitJson() {
        String dataSource = "flight_resource_60w.json";
        if (!FileUtil.exist(RESOUCE_FOLDER + dataSource)) {
            System.out.println("flight_resource_60w.json not found！");
            return;
        }
        List<FlightResourceDO> dataList = getFlightDataList(dataSource);
        File file = new File(RESOUCE_FOLDER + "flight_resource_5000.json");
        try {
            List<FlightResourceDO> data = CollUtil.split(dataList, 5000).get(2);
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writeValue(file, data);
        } catch (Exception e) {
            // ignore
        }
    }

    @Test
    public void testCache() {
        String dataSource = "flight_resource_5000.json";
        if (!FileUtil.exist(RESOUCE_FOLDER + dataSource)) {
            System.out.println("flight_resource_5000.json not found！");
            return;
        }

        // 项目启动时会缓存一些热点业务数据到内存，这些数据的特点是访问频繁但一般不做修改
        // 通常是基于一个hashmap来实现，如何设置key通常是按照业务要求来，比如第一个版本要求按照出发日期+出发地维度来缓存
        List<FlightResourceDO> dataList = getFlightDataList(dataSource);
        Map<String, List<FlightResourceDO>> hashMapCache1 = dataList.stream()
            .collect(groupingBy(e -> DateUtil.format(e.getDepartureTime(), DatePattern.PURE_DATE_PATTERN) + "&" + e.getDepartureCity()));
        // 后续业务迭代了，还有个地方要求按照出发日期+出发地+抵达地维度来缓存，简单的办法就是再增加一个hashmap
        Map<String, List<FlightResourceDO>> hashMapCache2 = dataList.stream()
            .collect(groupingBy(e -> DateUtil.format(e.getDepartureTime(), DatePattern.PURE_DATE_PATTERN) + "&" + e.getDepartureCity() + "&" + e.getArrivalCity()));
        // 可以看到每次新增一个查询维度，都需要重新编写代码新建一个hashmap，并增加了重复的数据，如果数据要做更新的话，两个map都要更新，还可能引入数据一致性问题

        // Trie可以解决上面的几个问题，下面是基于trie的实现
        Configuration configuration = new Configuration();
        // 出发日期
        CustomizedProperty<FlightResourceDO, Date> depDateProperty = new CustomizedProperty<>(FlightTrieIndexNames.INDEX_DEP_DATE, NodeType.TREE_MAP);
        depDateProperty.setPropertyMapper(FlightResourceDO::getDepartureTime);
        depDateProperty.setDictKeyMapper(r -> Integer.parseInt(DateUtil.format(r, DatePattern.PURE_DATE_PATTERN)));
        configuration.addProperty(depDateProperty);
        // 出发城市code
        SimpleProperty<FlightResourceDO, String> depCityCodeProperty = new SimpleProperty<>(FlightTrieIndexNames.INDEX_DEP_CITY_CODE, DictKeyType.INT);
        depCityCodeProperty.setPropertyMapper(FlightResourceDO::getDepartureCity);
        configuration.addProperty(depCityCodeProperty);
        // 抵达城市code
        SimpleProperty<FlightResourceDO, String> arrCityCodeProperty = new SimpleProperty<>(FlightTrieIndexNames.INDEX_ARR_CITY_CODE, DictKeyType.INT);
        arrCityCodeProperty.setPropertyMapper(FlightResourceDO::getArrivalCity);
        configuration.addProperty(arrCityCodeProperty);
        // 数据
        SimpleProperty<FlightResourceDO, FlightResourceDO> dataProperty = new SimpleProperty<>(FlightTrieIndexNames.DATA);
        dataProperty.setPropertyMapper(Function.identity());
        configuration.addProperty(dataProperty);
        // 插入数据
        Trie<FlightResourceDO> trie = new MapTrie<>(configuration);
        for (FlightResourceDO flightResourceDO : dataList) {
            trie.insert(flightResourceDO);
        }

        // 按照出发日期+出发地+抵达地查询
        FlightResourceDO randomData = RandomUtil.randomEle(dataList);
        Date depDate = randomData.getDepartureTime();
        String depCityCode = randomData.getDepartureCity();
        String arrCityCode = randomData.getArrivalCity();
        List<FlightResourceDO> trieResult = trie.dataSearch( //
            Criteria.where(FlightTrieIndexNames.INDEX_DEP_DATE).eq(depDate)//
                .and(FlightTrieIndexNames.INDEX_DEP_CITY_CODE).eq(depCityCode)//
                .and(FlightTrieIndexNames.INDEX_ARR_CITY_CODE).eq(arrCityCode)

        );
        trieResult.sort(Comparator.comparing(e -> DateUtil.format(e.getDepartureTime(), DatePattern.PURE_DATE_PATTERN) + "&" + e.getDepartureCity() + "&" + e.getArrivalCity()));

        List<FlightResourceDO> hashMapResult2 = hashMapCache2.get(DateUtil.format(depDate, DatePattern.PURE_DATE_PATTERN) + "&" + depCityCode + "&" + arrCityCode);
        hashMapResult2
            .sort(Comparator.comparing(e -> DateUtil.format(e.getDepartureTime(), DatePattern.PURE_DATE_PATTERN) + "&" + e.getDepartureCity() + "&" + e.getArrivalCity()));
        TestCase.assertTrue(CollectionUtil.isEqualList(trieResult, hashMapResult2));

        // 查询时间复杂度是一样的，同时解决了上面hashmap的几个问题，并且还有以下几个好处
        // 1.trie上的日期是范围查询的（logn的查询复杂度），hashmap不能支持范围查询
        // 2.trie中有字典的设计，可以复用，无论新增多少查询维度，数据字典都可以用同一个
        // 3.如果希望根据日期直接查询到出发地+抵达地的组合，hashmap需要拿到原始数据再组装一下，而trie可以直接从索引上查询出来（不需要遍历原始数据处理，性能更高），代码如下
        List<Pair> dataPairResult = dataList.stream() //
            .filter(e -> Objects.equals(DateUtil.format(e.getDepartureTime(), DatePattern.PURE_DATE_PATTERN), DateUtil.format(depDate, DatePattern.PURE_DATE_PATTERN))) //
            .map(e -> new Pair<>(e.getDepartureCity(), e.getArrivalCity())) //
            .distinct() //
            .collect(Collectors.toList());
        dataPairResult.sort(Comparator.comparing(e -> e.getKey() + "&" + e.getValue()));

        Criteria criteria = Criteria.where(FlightTrieIndexNames.INDEX_DEP_DATE).eq(depDate);

        ResultBuilder<Pair> resultBuilder = new ResultBuilder<>(Pair::new);
        resultBuilder.addSetter(FlightTrieIndexNames.INDEX_DEP_CITY_CODE, Pair::setKey);
        resultBuilder.addSetter(FlightTrieIndexNames.INDEX_ARR_CITY_CODE, Pair::setValue);
        List<Pair> triePairResult = trie.listSearch(criteria, null, resultBuilder);
        triePairResult.sort(Comparator.comparing(e -> e.getKey() + "&" + e.getValue()));

        TestCase.assertTrue(CollectionUtil.isEqualList(triePairResult, dataPairResult));
    }

    @Test
    public void testDictReUse() {
        String dataSource = "flight_resource_5000.json";
        if (!FileUtil.exist(RESOUCE_FOLDER + dataSource)) {
            System.out.println("flight_resource_5000.json not found！");
            return;
        }
        List<FlightResourceDO> dataList = getFlightDataList(dataSource);

        // 为什么将索引树和字典分离，是因为有时候想为一份数据建立多个索引，那么这多个索引是可以复用同一份字典数据的，可以节省空间
        // 出发日期
        CustomizedProperty<FlightResourceDO, Date> depDateProperty = new CustomizedProperty<>(FlightTrieIndexNames.INDEX_DEP_DATE, NodeType.TREE_MAP);
        depDateProperty.setPropertyMapper(FlightResourceDO::getDepartureTime);
        depDateProperty.setDictKeyMapper(r -> Integer.parseInt(DateUtil.format(r, DatePattern.PURE_DATE_PATTERN)));
        // 出发城市code
        SimpleProperty<FlightResourceDO, String> depCityCodeProperty = new SimpleProperty<>(FlightTrieIndexNames.INDEX_DEP_CITY_CODE, DictKeyType.INT);
        depCityCodeProperty.setPropertyMapper(FlightResourceDO::getDepartureCity);
        // 抵达城市code
        SimpleProperty<FlightResourceDO, String> arrCityCodeProperty = new SimpleProperty<>(FlightTrieIndexNames.INDEX_ARR_CITY_CODE, DictKeyType.INT);
        arrCityCodeProperty.setPropertyMapper(FlightResourceDO::getArrivalCity);
        // 数据
        SimpleProperty<FlightResourceDO, FlightResourceDO> dataProperty = new SimpleProperty<>(FlightTrieIndexNames.DATA);
        dataProperty.setPropertyMapper(Function.identity());

        // 假如第一个业务需要按照出发日期+出发城市+抵达城市+数据组织数据
        Configuration configuration1 = new Configuration();
        configuration1.addProperty(depDateProperty);
        configuration1.addProperty(depCityCodeProperty);
        configuration1.addProperty(arrCityCodeProperty);
        configuration1.addProperty(dataProperty);
        Trie<FlightResourceDO> trie1 = new MapTrie<>(configuration1);
        for (FlightResourceDO flightResourceDO : dataList) {
            trie1.insert(flightResourceDO);
        }

        // 第二个业务希望按照出发城市+出发日期+数据组织数据，这两个前缀树的字典是可以共享的（字典是Property的一个内部属性，公用Property就可以公用字典）
        Configuration configuration2 = new Configuration();
        configuration2.addProperty(depCityCodeProperty);
        configuration2.addProperty(depDateProperty);
        Trie<FlightResourceDO> trie2 = new MapTrie<>(configuration1);
        for (FlightResourceDO flightResourceDO : dataList) {
            trie2.insert(flightResourceDO);
        }

        for (int i = 0; i < 1000; i++) {
            FlightResourceDO randomData = RandomUtil.randomEle(dataList);
            Criteria criteria = Criteria.where(FlightTrieIndexNames.INDEX_DEP_DATE).eq(randomData.getDepartureTime());
            TestCase.assertEquals(trie1.dataSearch(criteria).size(), trie2.dataSearch(criteria).size());
        }
    }

    private List<FlightResourceDO> getFlightDataList(String dataSource) {
        File file = FileUtil.newFile(RESOUCE_FOLDER + dataSource);
        String json = FileUtil.readString(file, StandardCharsets.UTF_8);
        TypeReference<List<FlightResourceDO>> typeReference = new TypeReference<List<FlightResourceDO>>() {
        };
        return JSONUtil.toBean(json, typeReference, true);
    }

    private List<TrainSourceDO> getDataList(String dataSource) {
        File file = FileUtil.newFile(RESOUCE_FOLDER + dataSource);
        String json = FileUtil.readString(file, StandardCharsets.UTF_8);
        TypeReference<List<TrainSourceDO>> typeReference = new TypeReference<List<TrainSourceDO>>() {
        };
        List<TrainSourceDO> result = JSONUtil.toBean(json, typeReference, true);

        Date now = new Date();
        for (TrainSourceDO trainSourceDO : result) {
            if (Objects.isNull(trainSourceDO.getCreateDate())) {
                trainSourceDO.setCreateDate(now);
            }
        }
        return result;
    }

    private ResultBuilder buildResultBuilder() {
        ResultBuilder<TrainSourceDO> resultBuilder = new ResultBuilder<>(TrainSourceDO::new);
        Field[] fields = ReflectUtil.getFields(TrainSourceDO.class);
        for (Field field : fields) {
            resultBuilder.addSetter(field.getName(), (t, r) -> ReflectUtil.setFieldValue(t, field, r));
        }
        return resultBuilder;
    }

    private Configuration buildTrainQueryTrieConfiguration() {
        Configuration configuration = new Configuration();

        // 价格
        CustomizedProperty<TrainSourceDO, Double> priceProperty = new CustomizedProperty<>(TrainTrieIndexNames.INDEX_PRICE, NodeType.TREE_MAP);
        priceProperty.setPropertyMapper(TrainSourceDO::getMinRealPrice);
        priceProperty.setDictKeyMapper(r -> ((Double) (r * 100)).intValue());
        configuration.addProperty(priceProperty);

        // 出发城市id
        CustomizedProperty<TrainSourceDO, Integer> depCityIdProperty = new CustomizedProperty<>(TrainTrieIndexNames.INDEX_DEP_CITY_ID);
        depCityIdProperty.setPropertyMapper(TrainSourceDO::getDepartureCityId);
        depCityIdProperty.setDictKeyMapper(Integer::valueOf);
        configuration.addProperty(depCityIdProperty);

        // 抵达城市id
        CustomizedProperty<TrainSourceDO, Integer> arrCityIdProperty = new CustomizedProperty<>(TrainTrieIndexNames.INDEX_ARR_CITY_ID);
        arrCityIdProperty.setPropertyMapper(TrainSourceDO::getArrivalCityId);
        arrCityIdProperty.setDictKeyMapper(Integer::valueOf);
        configuration.addProperty(arrCityIdProperty);

        // 车次类型
        SimpleProperty<TrainSourceDO, String> trainTypeProperty = new SimpleProperty<>(TrainTrieIndexNames.INDEX_TRAIN_TYPE, DictKeyType.BYTE);
        trainTypeProperty.setPropertyMapper(TrainSourceDO::getTrainType);
        configuration.addProperty(trainTypeProperty);

        // 坐席类型
        SimpleProperty<TrainSourceDO, String> seatClassProperty = new SimpleProperty<>(TrainTrieIndexNames.INDEX_SEAT_CLASS, DictKeyType.BYTE);
        seatClassProperty.setPropertyMapper(TrainSourceDO::getSeatClass);
        configuration.addProperty(seatClassProperty);

        // 数据
        CustomizedProperty<TrainSourceDO, TrainSourceDO> dataProperty = new CustomizedProperty<>(TrainTrieIndexNames.DATA);
        dataProperty.setPropertyMapper(Function.identity());
        dataProperty.setDictKeyMapper(TrainSourceDO::getId);
        configuration.addProperty(dataProperty);
        return configuration;
    }

    private Configuration buildFlightQueryTrieConfiguration() {
        Configuration configuration = new Configuration();

        // 出发日期
        CustomizedProperty<FlightResourceDO, Date> depDateProperty = new CustomizedProperty<>(FlightTrieIndexNames.INDEX_DEP_DATE, NodeType.TREE_MAP);
        depDateProperty.setPropertyMapper(FlightResourceDO::getDepartureTime);
        depDateProperty.setDictKeyMapper(r -> Integer.parseInt(DateUtil.format(r, DatePattern.PURE_DATE_PATTERN)));
        configuration.addProperty(depDateProperty);

        // 价格
        CustomizedProperty<FlightResourceDO, Integer> priceProperty = new CustomizedProperty<>(FlightTrieIndexNames.INDEX_PRICE, NodeType.TREE_MAP);
        priceProperty.setPropertyMapper(FlightResourceDO::getLcp);
        priceProperty.setDictKeyMapper(r -> r);
        configuration.addProperty(priceProperty);

        // 出发城市code
        SimpleProperty<FlightResourceDO, String> depCityCodeProperty = new SimpleProperty<>(FlightTrieIndexNames.INDEX_DEP_CITY_CODE, DictKeyType.INT);
        depCityCodeProperty.setPropertyMapper(FlightResourceDO::getDepartureCity);
        configuration.addProperty(depCityCodeProperty);

        // 抵达城市code
        SimpleProperty<FlightResourceDO, String> arrCityCodeProperty = new SimpleProperty<>(FlightTrieIndexNames.INDEX_ARR_CITY_CODE, DictKeyType.INT);
        arrCityCodeProperty.setPropertyMapper(FlightResourceDO::getArrivalCity);
        configuration.addProperty(arrCityCodeProperty);

        // 舱等类型
        CustomizedProperty<FlightResourceDO, Integer> cabinClassProperty = new CustomizedProperty<>(FlightTrieIndexNames.INDEX_CABIN_TYPE, NodeType.TREE_MAP);
        cabinClassProperty.setPropertyMapper(FlightResourceDO::getCabinType);
        cabinClassProperty.setDictKeyMapper(r -> r);
        configuration.addProperty(cabinClassProperty);

        // 数据
        SimpleProperty<FlightResourceDO, FlightResourceDO> dataProperty = new SimpleProperty<>(FlightTrieIndexNames.DATA);
        dataProperty.setPropertyMapper(Function.identity());
        configuration.addProperty(dataProperty);

        return configuration;
    }

    private Configuration buildConfiguration3(Class<?> clazz) {
        Configuration configuration = new Configuration();
        Field[] fields = ReflectUtil.getFields(clazz);
        for (Field field : fields) {
            if (Number.class.isAssignableFrom(field.getType())) {
                CustomizedProperty customizedProperty = new CustomizedProperty<>(field.getName(), NodeType.TREE_MAP);
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
        configuration.setUseFastErase(true);
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

        // 出发站点
        SimpleProperty<TrainSourceDO, String> departureStationCodeProperty = new SimpleProperty<>("departureStationCode", DictKeyType.INT);
        departureStationCodeProperty.setPropertyMapper(TrainSourceDO::getDepartureStationCode);
        configuration.addProperty(departureStationCodeProperty);

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

        // 最低票价
        CustomizedProperty<TrainSourceDO, Integer> minRealPriceProperty = new CustomizedProperty<>("minRealPrice", NodeType.TREE_MAP);
        minRealPriceProperty.setPropertyMapper(e -> Double.valueOf(e.getMinRealPrice()).intValue());
        minRealPriceProperty.setDictKeyMapper(r -> r);
        configuration.addProperty(minRealPriceProperty);

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

    private void triggerGc() {
        Runtime.getRuntime().gc();
        Runtime.getRuntime().runFinalization();
        Runtime.getRuntime().gc();
        Runtime.getRuntime().runFinalization();
        Runtime.getRuntime().gc();
        Runtime.getRuntime().runFinalization();
        Runtime.getRuntime().gc();
        Runtime.getRuntime().runFinalization();
        Runtime.getRuntime().gc();
        Runtime.getRuntime().runFinalization();
        Runtime.getRuntime().gc();
        Runtime.getRuntime().runFinalization();
        Runtime.getRuntime().gc();
        Runtime.getRuntime().runFinalization();
        Runtime.getRuntime().gc();
        Runtime.getRuntime().runFinalization();
        Runtime.getRuntime().gc();
        Runtime.getRuntime().runFinalization();
        Runtime.getRuntime().gc();
        Runtime.getRuntime().runFinalization();
        Runtime.getRuntime().gc();
        Runtime.getRuntime().runFinalization();
        Runtime.getRuntime().gc();
        Runtime.getRuntime().runFinalization();
        Runtime.getRuntime().gc();
        Runtime.getRuntime().runFinalization();
        Runtime.getRuntime().gc();
        Runtime.getRuntime().runFinalization();
    }

    private void printMemoryUse() {
        System.out.println("Memory Usages:");
        //当前JVM占用的内存总数(M)
        double total = (Runtime.getRuntime().totalMemory()) / (1024.0 * 1024);
        //JVM最大可用内存总数(M)
        double max = (Runtime.getRuntime().maxMemory()) / (1024.0 * 1024);
        //JVM空闲内存(M)
        double free = (Runtime.getRuntime().freeMemory()) / (1024.0 * 1024);
        //可用内存内存(M)
        double mayuse = (max - total + free);
        //已经使用内存(M)
        double used = (total - free);
        System.out.println("Used: " + ((Double) used).intValue() + "MB");
    }

}