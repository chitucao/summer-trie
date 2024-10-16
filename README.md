# summer-trie

## 项目地址

github	https://github.com/chitucao/summer-trie.git

gitee 	  https://gitee.com/chitucao/summer-trie.git

## 介绍

​	这是一个适用于数据索引、缓存、数据压缩的前缀树工具，比较轻量易用。支持完整的增删改查操作，以及一些特殊的查询方式（如非前缀匹配、树查询、聚合查询），支持序列化。

​	不同于有限字符集前缀树实现（比如只存储单词，有限字符集内），主要考虑的是如何简单方便地组织数据索引，支持尽可能多的查询方式和兼顾删除效率，但是在空间复杂度上也做了一些优化；

​	目前是用于我司机票、火车票盲盒的线路资源筛选流程、数据分析以及一些查询业务的结果缓存；

​	如果使用的过程中发现有bug，或者希望添加额外的功能，欢迎提交PR;



### 适用于以下场景

- 索引：为内存数据建立索引，提高查询效率；
- 缓存：可以业务数据中提取出需要的字段，组成一个树缓存；
- 数据压缩，可以将列表数据转换成树结构存储，配合字段值字典，可以压缩数据，也提供了更多的查询方式；



### 关键功能和特性

- 比较通用和易用，只需要编写少量的代码，就可以基于数据构建前缀树；
- 支持多种方式的增删改查；
- 不仅仅是前缀查询（或者说查询和展示字段没有顺序关系，任意层级既可以作为查询条件，也可以作为展示字段）；
- 查询条件支持EQUAL、BETWEEN、GTE、LTE、IN、NOT_IN（可以扩展），得益于树结构，等值查询复杂度可以支持到O(1)，范围查询可以支持到O(logn)，不仅仅是前缀匹配，也可以支持后续条件匹配；
- 支持简单的聚合查询（可以扩展），例如MIN，MAX，时间复杂度可以达到O(logn)；
- 树节点不存储实际字段值，而是由一个全局的字典维护字段值，树节点存储的是字段对应的字典索引，可以压缩数据，提高查询性能；
- 支持Protobuf序列化和反序列化，适用于通信和数据dump分析;



### 几种核心的查询方式

- 按层查询，可以查询某一层的数据，典型的应用场景是从首层到底层依次查询情况；
- 原始数据查询，属于按层查询的特殊情况，查询的是叶子节点，并且叶子节点存储的是原始数据的情况；
- 树结构查询，指定要查询的多个索引字段，并将查询结果组合成一颗新树返回，支持聚合；
- 列表结构查询，列表是数据最常用的展示方式，查询过程同树结构查询，不过会将树结构平铺成列表返回；
- 字典值查询，字典维护了某个字段的所有有效值，当查询时不知道如何入手时，可以从字典值范围内选取；



## 核心概念

### 节点（Node）

- 节点分为根节点、树枝节点、叶子节点，所有的节点都不存实际的字段值，而是存储number类型的字典key；
- 节点可以存储原始数据的某个字段，也可以存储多个字段的组合和映射（如成对出现的日期范围）；
- 叶子节点也可以不存储原始数据，适用于树中的字段已经能够满足查询条件的情况，比如一个索引不够想多做几个辅助查询；
- 节点的类型目前有两种，treeMap和hashMap，如果想做范围和比较查询，例如价格，金额，日期等，就用treeMap，如果字段是枚举类型，只用到等值、多值查询，用hashMap足够，其次是增删改性能的差别；



### 字典（Dict）

- 前缀树后续层级的节点中，同一个字段值可能会出现多次，如果直接将重复的值存储在树节点上，会比较浪费空间，所以想到为每个字段建立一个全局唯一的映射关系，也就是字典，字典的key对应一个number类型的id，字典的value对应实际的值，树节点上只需要存储字典key就可以了，可以节省空间；
- 这个映射关系主要分为两类，一种是类数字类型，比如字段是日期，金额，数字编号等，可以唯一转换成一个number类型的字典key，这样做的好处是可以做范围查询，另一种是数据本身无法对应这种数字类型的id，是由字典去分配一个自增id，这种情况下这个字段是不支持范围查询的；
- 所以树节点上存储的实际上就是字典key，是number类型的，只要是number的子类就行，如果字典范围比较小，可以尽量映射成范围较小的数据结构节省空间；
- 选用number类型的另外一个好处是一般支持comparable接口，这样节点就可以使用sortedMap，比如treeMap，在例如金额的范围，比较，聚合查询的时候更有优势，对比hashMap，可以降低时间复杂度；
- 字典是和字段绑定的，并且是支持复用的，如果两棵前缀树用到了同一个字段，用同一个字典也行，可以节省空间。典型的应用场景为一份数据建立多个前缀树索引的场景，最后数据节点的字典是可以复用的；



### 属性（Property）

- 属性描述了选取数据实体的哪个字段作为一个树节点，并指定了这个字段值和字典key的映射关系；
- 属性的类型目前有两种，一种是simpleProperty，适用于枚举类型，不需要比较和范围查询，另一种CustomProperty，可以手动指定和字典key的映射关系，映射成一个数字，可以支持范围和比较查询；
- Property是支持复用的，Dict是Property的内部变量，所以共享了Property就相当于共享了字典；



### 配置（Configuration）

- 配置描述了字典树的建立，有哪些字段，字段的顺序关系，是否需要快速删除等等；
- configuration还提供了一个特殊的方法，sortProperties方法，如果对按添加顺序构建节点不满意，可以在这里调整；





## 快速开始

### 添加maven坐标

```xml
<dependency>
    <groupId>top.chitucao.summerframework</groupId>
    <artifactId>summer-trie</artifactId>
    <version>1.0.4.RELEASE</version>
</dependency>
```

### 新建前缀树

#### 1.作为索引，并查询原始数据

- 比如一个对象叫TrainSourceDO，是一个火车票资源地实体，希望为出发城市、抵达城市、价格、坐席类型建立索引，并且能够查询到数据本身，可以按照以下配置；

- 这里的setPropertyMapper是指定希望哪个字段作为要建立索引的字段，setDictKeyMapper指定了索引字段和字典key（树上实际存储的数据）映射关系；

- 这里的价格我们希望是能够支持范围和比较查询的，并且为了提高查询性能，所以指定了用treeMap；

- 最终前缀树节点的顺序是按照添加顺序来的，也就是出发城市作为第一个节点，原始数据作为尾部节点，所以是可以查询原始数据的；

  ```java
  Configuration configuration = new Configuration();
  // 出发城市
  CustomizedProperty<TrainSourceDO, Integer> depCityIdProperty = new CustomizedProperty<>("depCityId");
  depCityIdProperty.setPropertyMapper(TrainSourceDO::getDepartureCityId);
  depCityIdProperty.setDictKeyMapper(r -> r);
  configuration.addProperty(depCityIdProperty);
  
  // 抵达城市
  CustomizedProperty<TrainSourceDO, Integer> arrCityIdProperty = new CustomizedProperty<>("arrCityId");
  arrCityIdProperty.setPropertyMapper(TrainSourceDO::getArrivalCityId);
  arrCityIdProperty.setDictKeyMapper(r -> r);
  configuration.addProperty(arrCityIdProperty);
  
  // 价格
  CustomizedProperty<TrainSourceDO, Integer> arrDistrictIdProperty = new CustomizedProperty<>("price", NodeType.TREE_MAP);
  arrDistrictIdProperty.setPropertyMapper(t -> ((Double) t.getMinRealPrice()).intValue());
  arrDistrictIdProperty.setDictKeyMapper(r -> r);
  configuration.addProperty(arrDistrictIdProperty);
  
  // 坐席类型
  SimpleProperty<TrainSourceDO, String> seatClassProperty = new SimpleProperty<>("seatClass", DictKeyType.BYTE);
  seatClassProperty.setPropertyMapper(TrainSourceDO::getSeatClass);
  configuration.addProperty(seatClassProperty);
  
  // 数据
  CustomizedProperty<TrainSourceDO, TrainSourceDO> dataProperty = new CustomizedProperty<>("data");
  dataProperty.setPropertyMapper(Function.identity());
  dataProperty.setDictKeyMapper(TrainSourceDO::getId);
  configuration.addProperty(dataProperty);
  
  // 新建前缀树
  MapTrie<TrainSourceDO> trie = new MapTrie<>(configuration);
  ```

#### 2.用作索引，只查询索引

- 同上，只是尾部节点不再是数据了；

  ```java
  Configuration configuration = new Configuration();
  // 出发城市
  CustomizedProperty<TrainSourceDO, Integer> depCityIdProperty = new CustomizedProperty<>("depCityId");
  depCityIdProperty.setPropertyMapper(TrainSourceDO::getDepartureCityId);
  depCityIdProperty.setDictKeyMapper(r -> r);
  configuration.addProperty(depCityIdProperty);
  
  // 抵达城市
  CustomizedProperty<TrainSourceDO, Integer> arrCityIdProperty = new CustomizedProperty<>("arrCityId");
  arrCityIdProperty.setPropertyMapper(TrainSourceDO::getArrivalCityId);
  arrCityIdProperty.setDictKeyMapper(r -> r);
  configuration.addProperty(arrCityIdProperty);
  
  // 新建前缀树
  MapTrie<TrainSourceDO> trie = new MapTrie<>(configuration);
  ```

#### 3.用于压缩

- 将所有字段都作为前缀树的节点，反射构建；

  ```java
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
  // 新建前缀树
  MapTrie<TrainSourceDO> trie = new MapTrie<>(configuration);
  ```

- 再利用resultBuilder可以将数据完整的查询出来，反射构建；

  ```java
  ResultBuilder<TrainSourceDO> resultBuilder = new ResultBuilder<>(TrainSourceDO::new);
  Field[] fields = ReflectUtil.getFields(TrainSourceDO.class);
  for (Field field : fields) {
      resultBuilder.addSetter(field.getName(), (t, r) -> ReflectUtil.setFieldValue(t, field, r));
  }
  return resultBuilder;
  ```

- 不带虚拟头节点的话，前缀树就是一个梯形结构，所以将选择性比较高的字段排在后面能够更好的压缩数据，可以先建立一次前缀树，拿到所有字段的字典值大小排序后再按照大小重新构建一次；

  ```java
  // 拿到每个字段的字典值大小
  Map<String, Integer> dictSizes = trie1.dictSizes();
  // 按照字段的大小排序
  CollectionUtil.sort(configuration2.getProperties(), Comparator.comparing(e -> dictSizes.get(e.name())));
  // 重新构建
  MapTrie<TrainSourceDO> trie2 = new MapTrie<>(configuration2);
  for (TrainSourceDO data : dataList) {
      trie2.insert(data);
  }
  ```



### 添加数据

- 直接insert就行，时间复杂度是O(h)，h是前缀树高度，也就是建立索引的字段数量

  ```java
  List<TrainSourceDO> dataList = getDataList("train_resource_3000.json");
  for (TrainSourceDO data : dataList) {
      trie.insert(data);
  }
  ```



### 删除数据

- 1.根据数据本身删除；

  - 数据本身包含了所有建立索引的字段，所以删除效率较高，复杂度O(h)

    ```java
    List<TrainSourceDO> dataToErase = RandomUtil.randomEles(dataList, 10);
    for (TrainSourceDO data : dataToErase) {
        trie.erase(data);
    }
    ```

- 2.根据条件删除

  - 尽量给出尽可能多的字段，可以提高删除的效率，给出首部的字段删除效率高一点，直接给出尾部的字段需要循环查找，删除效率低；

    ```java
    Criteria criteria = new Criteria().addCriterion(Condition.BETWEEN, 0, 1005, "depCityId");
    trie.erase(criteria);
    ```

  - 有一种特殊情况，如果希望只根据id删除，然后尽量提高删除的效率的话，可以先根据尾部节点的字典拿到该数据，然后再删除，时间复杂度O(1)+O(h)；

    ```java
    // 数据
    CustomizedProperty<TrainSourceDO, TrainSourceDO> dataProperty = new CustomizedProperty<>("data", NodeType.TREE_MAP);
    dataProperty.setPropertyMapper(Function.identity());
    dataProperty.setDictKeyMapper(TrainSourceDO::getId);
    configuration.addProperty(dataProperty);
    
    // 先根据字典拿到数据，然后再删除
    long id = 143859138L;
    TrainSourceDO eraseData = (TrainSourceDO) trie.dictValues("data", id).iterator().next();
    trie.erase(eraseData);
    ```



### 查询数据

#### 1.按层查询

- 比如盲盒场景中，用户都是选择出发地然后随机出发日期，这里出发地是固定的，所以可以以出发地为条件，拿到所有的有效出发日期，然后再拿到有效出发日期下面的抵达地什么的；

```java
List<Integer> queryDepCityList = Lists.newArrayList(144, 145, 146, 900);
List<Integer> indexList1 = dataList.stream().filter(e -> queryDepCityList.contains(e.getDepartureCityId())).map(TrainSourceDO::getArrivalCityId).distinct().sorted()
            .collect(Collectors.toList());

Criteria criteria = new Criteria();
criteria.addCriterion(Condition.IN, queryDepCityList, "depCityId");
List<Integer> indexList2 = trie.<Integer> propertySearch(criteria, "arrCityId").stream().sorted().collect(Collectors.toList());
```

#### 2.原始数据查询

- 一般来说是不会为数据的所有字段建立索引的，如果希望取到到非索引字段做一些操作，可以用这个方法直接查询最后一层原始数据；

```shell
Criteria criteria = new Criteria();
criteria.addCriterion(Condition.IN, queryDepCityList, "depCityId");
List<TrainSourceDO> dataList2 = trie.dataSearch(criteria).stream().sorted(Comparator.comparing(TrainSourceDO::getId)).collect(Collectors.toList());
```

#### 3.树结构查询

- 树结构看起来比较直观，可以指定多个要查询的字段，组合成一颗树返回（这个树结构依然保持了原始数据字段间的关系）；

- 支持聚合（对比mysql你可以这样理解，聚合字段前面的字段都是group by条件。支持多个字段的聚合）；

- 你可以理解这个返回结果是多个hashmap的嵌套，最后一层是一个arrayList；

  ```shell
  Criteria criteria = new Criteria();
  criteria.addCriterion(Condition.IN, queryDepCityList, "depCityId");
  Aggregations aggregations = new Aggregations();
  Object result = trie.treeSearch(criteria, aggregations, "depCityId", "arrCityId", "id");
  ```

#### 4.列表结构查询

- 同树结构查询，不过平铺成了列表，列表是最常用的数据返回方式；

- 是支持聚合的，比如例如出发城市、抵达城市、出发日期、价格构建一颗树，然后对价格使用最小值聚合，就用字典树实现了低价日历，以下是伪代码；

  时间复杂度是O(1) x O(1) x O(log(d)) x O(log(p))   d是出发抵达地下的有效日期天数，p是有效日期下的价格总数
  
  ```java
  Trie<FlightUnitVO> trie = inlandFlightTrieIndexManager.getTrie();
  Criteria criteria = buildCriteriaByQueryCondition(request.getQueryCondition());
  
  Aggregations aggregations = new Aggregations();
  aggregations.addAggregation(Aggregation.MIN, FlightTrieIndexNames.INDEX_PRICE);
  
  ResultBuilder<ListSearchResponse> resultBuilder = new ResultBuilder<>(ListSearchResponse::new);
  resultBuilder.addSetter(FlightTrieIndexNames.INDEX_DEP_CITY_CODE, ListSearchResponse::setDepCityCode);
  resultBuilder.addSetter(FlightTrieIndexNames.INDEX_ARR_CITY_CODE, ListSearchResponse::setArrCityCode);
  resultBuilder.addSetter(FlightTrieIndexNames.INDEX_DEP_DATE, ListSearchResponse::setDate);
  resultBuilder.addSetter(FlightTrieIndexNames.INDEX_PRICE, ListSearchResponse::setMinPrice);
  
  List<ListSearchResponse> result = trie.listSearch(criteria, aggregations, resultBuilder);
  return R.ok(result);
  ```

#### 5.字典值查询

- 常见的情况是作为下拉框的options；

  ```shell
  List<Integer> dataList2 = trie.<Integer>dictValues("depCityId").stream().sorted().collect(Collectors.toList());
  ```

- 下面是字典的一种特殊玩法，如果希望通过id查询，没必要从树上查，可以直接从最后那个数据字典拿，O(1)的复杂度；

  ```shell
  long id = 143859138L;
  TrainSourceDO eraseData = (TrainSourceDO) trie.dictValues("data", id).iterator().next();
  ```



### 实现业务数据缓存

- 项目启动时会缓存一些热点业务数据到内存，这些数据的特点是访问频繁但一般不做修改

- 通常是基于一个hashmap来实现，如何设置key通常是按照业务要求来

  - 比如第一个版本要求按照出发日期+出发地维度来缓存

    ```java
    List<FlightResourceDO> dataList = getFlightDataList(dataSource);
    Map<String, List<FlightResourceDO>> hashMapCache1 = dataList.stream()
        .collect(groupingBy(e -> DateUtil.format(e.getDepartureTime(), DatePattern.PURE_DATE_PATTERN) + "&" + e.getDepartureCity()));
    ```

  - 后续业务迭代了，还有个地方要求按照出发日期+出发地+抵达地维度来缓存，简单的办法就是再增加一个hashmap

    ```
    Map<String, List<FlightResourceDO>> hashMapCache2 = dataList.stream()
        .collect(groupingBy(e -> DateUtil.format(e.getDepartureTime(), DatePattern.PURE_DATE_PATTERN) + "&" + e.getDepartureCity() + "&" + e.getArrivalCity()));
    ```

- 这样使用hashmap来实现会带来以下几个问题

  - 每次新增一个查询维度，都需要重新编写代码新建一个hashmap，需要重复编码；
  - 每个hashmap的value存储的是同一份数据，数据重复了；
  - 如果数据要做更新的话，两个map都要更新，可能引入数据一致性问题；

- Trie可以解决上面的几个问题，下面是基于trie的实现

  ```java
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
  ```

- 具体的数据查询实现：

  ```java
  // 按照出发日期+出发地+抵达地查询
  FlightResourceDO randomData = RandomUtil.randomEle(dataList);
  Date depDate = randomData.getDepartureTime();
  String depCityCode = randomData.getDepartureCity();
  String arrCityCode = randomData.getArrivalCity();
  List<FlightResourceDO> trieResult = trie.dataSearch(new Criteria() //
      .addCriterion(Condition.EQUAL, depDate, FlightTrieIndexNames.INDEX_DEP_DATE) //
      .addCriterion(Condition.EQUAL, depCityCode, FlightTrieIndexNames.INDEX_DEP_CITY_CODE) //
      .addCriterion(Condition.EQUAL, arrCityCode, FlightTrieIndexNames.INDEX_ARR_CITY_CODE) //
  );
  trieResult.sort(Comparator.comparing(e -> DateUtil.format(e.getDepartureTime(), DatePattern.PURE_DATE_PATTERN) + "&" + e.getDepartureCity() + "&" + e.getArrivalCity()));
  List<FlightResourceDO> hashMapResult2 = hashMapCache2.get(DateUtil.format(depDate, DatePattern.PURE_DATE_PATTERN) + "&" + depCityCode + "&" + arrCityCode);
  hashMapResult2
      .sort(Comparator.comparing(e -> DateUtil.format(e.getDepartureTime(), DatePattern.PURE_DATE_PATTERN) + "&" + e.getDepartureCity() + "&" + e.getArrivalCity()));
  TestCase.assertTrue(CollectionUtil.isEqualList(trieResult, hashMapResult2));
  ```

- trie实现的查询时间复杂度是一样的，同时解决了上面hashmap的几个问题

  - 每次新增一个查询维度，只需要新增一个property就好，代码改动很小；
  - 多个查询维度可以公用一份数据，不会带来重复的数据，也不会引入一致性问题；

- 同时trie还支持额外的查询方式

  - trie上的日期是范围查询的（logn的查询复杂度），hashmap只能做等值查询；

  - 如果希望根据日期直接查询到出发地+抵达地的组合，hashmap需要拿到原始数据再组装一下，而trie可以直接从索引上查询出来（不需要遍历原始数据处理，性能更高），代码如下

    ```java
    // hashmap实现
    List<MyPair> dataPairResult = dataList.stream() //
        .filter(e -> Objects.equals(DateUtil.format(e.getDepartureTime(), DatePattern.PURE_DATE_PATTERN), DateUtil.format(depDate, DatePattern.PURE_DATE_PATTERN))) //
        .map(e -> new MyPair(e.getDepartureCity(), e.getArrivalCity())) //
        .distinct() //
        .collect(Collectors.toList());
    dataPairResult.sort(Comparator.comparing(e -> e.getKey() + "&" + e.getVal()));
    
    // trie实现
    Criteria criteria = new Criteria().addCriterion(Condition.EQUAL, depDate, FlightTrieIndexNames.INDEX_DEP_DATE);
    ResultBuilder<MyPair> resultBuilder = new ResultBuilder<>(MyPair::new);
    resultBuilder.addSetter(FlightTrieIndexNames.INDEX_DEP_CITY_CODE, MyPair::setKey);
    resultBuilder.addSetter(FlightTrieIndexNames.INDEX_ARR_CITY_CODE, MyPair::setVal);
    List<MyPair> triePairResult = trie.listSearch(criteria, null, resultBuilder);
    triePairResult.sort(Comparator.comparing(e -> e.getKey() + "&" + e.getVal()));
    
    TestCase.assertTrue(CollectionUtil.isEqualList(triePairResult, dataPairResult));
    ```



### 多索引情况下的字典复用

- 为什么将索引树和字典分离，是因为有时候想为一份数据建立多个字典树索引，那么这多个索引如果能公用一份字典数据，是可以节省空间的；

- 字典是Property的一个内部属性，公用Property就可以公用字典，字典是支持扩展的，可以作为一个构造参数传入Property；

  - 定义几个复用的索引字段

    ```java
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
    ```

  - 假如第一个业务需要按照出发日期+出发城市+抵达城市+数据组织数据

    ```java
    Configuration configuration1 = new Configuration();
    configuration1.addProperty(depDateProperty);
    configuration1.addProperty(depCityCodeProperty);
    configuration1.addProperty(arrCityCodeProperty);
    configuration1.addProperty(dataProperty);
    Trie<FlightResourceDO> trie1 = new MapTrie<>(configuration1);
    for (FlightResourceDO flightResourceDO : dataList) {
        trie1.insert(flightResourceDO);
    }
    ```

  - 第二个业务希望按照出发城市+出发日期+数据组织数据，那么可以使用同一份Property

    ```java
    Configuration configuration1 = new Configuration();
    configuration1.addProperty(depDateProperty);
    configuration1.addProperty(depCityCodeProperty);
    configuration1.addProperty(arrCityCodeProperty);
    configuration1.addProperty(dataProperty);
    Trie<FlightResourceDO> trie1 = new MapTrie<>(configuration1);
    for (FlightResourceDO flightResourceDO : dataList) {
        trie1.insert(flightResourceDO);
    }
    
    // 单元测试
    for (int i = 0; i < 1000; i++) {
        FlightResourceDO randomData = RandomUtil.randomEle(dataList);
        Criteria criteria = new Criteria().addCriterion(Condition.EQUAL, randomData.getDepartureTime(), FlightTrieIndexNames.INDEX_DEP_DATE);
        TestCase.assertEquals(trie1.dataSearch(criteria).size(), trie2.dataSearch(criteria).size());
    }
    ```



### 序列化和反序列化

- 使用的是protobuf序列化，对比原始json数据，序列化后的大小为原始json的1/5，也适用于数据dump分析；

  ```java
  // 序列化
  MapTrie<TrainSourceDO> trie1 = new MapTrie<>(buildConfiguration3());
  for (TrainSourceDO data : dataList) {
      trie1.insert(data);
  }
  File dumpFile = new File(RESOUCE_FOLDER + "train_resource_dump.dat");
  if (dumpFile.exists()) {
      dumpFile.delete();
  }
  FileUtil.writeBytes(trie1.serialize(), dumpFile);
  
  // 反序列化
  MapTrie<TrainSourceDO> trie2 = new MapTrie<>(buildConfiguration3());
  trie2.deserialize(FileUtil.readBytes(dumpFile));
  ```





## 性能指标

|                        | 数据量 | 索引数量 | 内存构建耗时 | 反序列化构建耗时 | 堆内存总占用 | 索引占用堆内存 | 数据占用堆内存 |
| ---------------------- | ------ | -------- | ------------ | ---------------- | ------------ | -------------- | -------------- |
| 火车票通用查询前缀树   | 36w    | 6        | 958 ms       | 3063  ms         | 179  MB      | 32  MB         | 147  MB        |
| 国内机票通用查询前缀树 | 55w    | 6        | 2232  ms     | 5679  ms         | 799  MB      | 156  MB        | 643  MB        |
