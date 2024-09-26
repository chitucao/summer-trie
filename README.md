# summer-trie

## 项目地址

github	https://github.com/chitucao/summer-trie.git

gitee 	  https://gitee.com/chitucao/summer-trie.git

## 介绍

​	这是一个节点支持任意数据类型的前缀树，支持非前缀匹配，支持树查询、聚合查询等多种特殊的查询方式。设计得比较轻量通用，适用于大量列表数据的索引和压缩。不同于有限字符集前缀树实现（每个节点表达的状态是同一类型），主要是设计思想是将数据中多个不同类型的字段作为节点，组合成一颗前缀树，提高这些字段的检索性能；

​	目前是用于同程旅行盲盒机票、火车票的本地资源预筛选、数据分析以及校验场景下的结果缓存；

​	如果使用的过程中发现有bug，或者希望添加额外的功能，欢迎提交PR;



### 适用于以下场景

- 索引，大量列表数据场景下建立前缀树索引，提高检索性能；
- 数据压缩，支持将列表数据转换成树结构，配合字段值字典，既压缩了数据，也提高了查询性能；



### 关键功能和特性

- 比较通用和易用，只需要编写少量的代码，就可以将某个数据的多个字段组合成前缀树；
- 支持非前缀匹配，支持后续字段反向查询前缀字段（或者说查询和展示字段没有顺序关系，任意层级既可以作为查询条件，也可以作为展示字段）；
- 支持多种方式的增删改查；
- 查询条件支持EQUAL、BETWEEN、GTE、LTE、IN、NOT_IN（可以扩展），得益于树结构，等值查询复杂度可以支持到O(1)，范围查询可以支持到O(logn)，不仅仅是前缀匹配，也可以支持后续条件匹配；
- 支持简单的聚合查询（可以扩展），例如MIN，MAX，时间复杂度可以达到O(logn)；
- 树节点不存储实际字段值，而是由一个全局的字典维护字段值，树节点存储的是字段对应的字典索引，可以压缩数据，提高查询性能；
- 支持序列化和反序列化，适用于通信和数据dump分析;



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
- 所以树节点上存储的实际上就是字典key，是number类型的，只要是number的子类就行，如果字典范围比较小，可以尽量映射成范围较小的数据结构洁身空间；
- 选用number类型的另外一个好处是一般支持comparable接口，这样节点就可以使用sortedMap，比如treeMap，在例如金额的范围，比较，聚合查询的时候更有优势，对比hashMap，可以降低时间复杂度；
- 字典是和字段绑定的，并且是支持复用的，如果两棵前缀树用到了同一个字段，用同一个字典也行，可以节省空间。典型的应用场景为一份数据建立多个前缀树索引的场景，最后数据节点的字典是可以复用的；



### 属性（Property）

- 属性描述了选取数据实体的哪个字段作为一个树节点，并指定了这个字段值和字典key的映射关系；
- 属性的类型目前有两种，一种是simpleProperty，适用于枚举类型，不需要比较和范围查询，另一种CustomProperty，可以手动指定和字典key的映射关系，映射成一个数字，可以支持范围和比较查询；
- 注意simpleProperty是不能共享的，simpleProperty内部有一个局部变量（自增id），customProperty支持共享；



### 配置（Configuration）

- 配置描述了字典树的建立，有哪些字段，字段的顺序关系，是否需要快速删除等等；
- configuration还提供了一个特殊的方法，sortProperties方法，如果对按添加顺序构建节点不满意，可以在这里调整；





## 快速开始

### 添加maven坐标

```xml
<dependency>
    <groupId>top.chitucao.summerframework</groupId>
    <artifactId>summer-trie</artifactId>
    <version>1.0.2.RELEASE</version>
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





## 性能分析和对比

### 前缀树和位图

#### 树

- 树中的索引条目和行之间是一对一的关系；
- 适用于大量的增删改查；
- 适用于选择性高的列；

#### 位图

- 位图的一个索引条目对应多条数据，每个bit位对应一行，占用空间非常小，很好地利用了CPU Cache的空间局部性；
- 适用于高度重复且读多写少的数据（低基数，选择性低）；
- 适合OR和AND操作；
- update成本比较高；
- 最早项目中用的就是位图，类似倒排索引，位图存在这样几个问题；
  - 位图并不是很适合为价格，日期这种基数比较高的字段建立索引，所以一般针对价格，日期的查询，还需要拿到原始数据再进行一次遍历后查询；
  - 如果一个字段，比如出发城市有100个，那么每新增一个城市就需要新增一个位图，这个位图如果不压缩的话，占用空间还是很大的，比如现在有60w的数据，那么每个索引在不压缩的情况下就是600000/8/1024 = 73kb；
  - 位图更新的成本比较高，因为涉及到多个索引，直接更新还可能会引入一致性问题，所以一般不会直接更新原始的数据。拿es来举例，删除或者更新会将原始的文档id标记为删除，然后新增，最后在段合并的时候将这些无效的数据清理掉。所以更新的次数多了，位图中就会出现很多无效的占用空间的数据，导致空间占用越来越高，所以还需要在业务低峰期定期重建索引；



### 前缀树和B+树

#### 相同点

- 都是多路查找树；
- 查询效率都比较稳定；
- 数据都是存在叶子节点上；
- 非叶子结点相当于是叶子结点的索引；

#### 不同点

- B+树是整体有序的，适合于范围和等值查找，层数和和阶数有关，每个节点或者多个节点的数据尽量分布均匀并且放在一起，InnoDB里面Page是存储单位（空间局部性原理和磁盘预读）；
- 前缀树是一般是整体无序的（单个节点内可以实现有序），数据并不要求均匀分布在每个节点上，或者将多个节点的数据放在一起组成固定大小。数据存的比较分散的坏处是多了很多的引用（指针），增加了空间复杂度，并且不适用于空间局部性，但这样组织很适合于等值查询。这个项目的实现里，层数是固定的，每个字段按层分配和查找，好处是可以实现非前缀匹配；

- Trie不适用于存储在随机访问比较慢的介质上，当数据非常庞大并且存储在磁盘上时，数据结构的效率更多地取决于磁盘块访问的数量，而不是所有操作的总量。B+ 数据在一个节点（可视为“数据块”）中包含许多记录，因此所需的块访问次数比 Trie 少得多；
- B+树更适合操作系统的文件索引和数据库索引；
- B+树的叶子节点增加了链指针，主要是为了加快检索多个相邻叶节点的效率，可以实现顺序查找；



## 前缀树的种类和变种

- prefix tree；
- suffix tree；
- radix tree(patricia tree, compact prefix tree)；
- crit-bit tree；
- double array trie；
- ternary trie；
- Kart-trie；











