package top.chitucao.summerframework.trie;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import top.chitucao.summerframework.trie.configuration.Configuration;
import top.chitucao.summerframework.trie.configuration.property.impl.AutoMappingProperty;
import top.chitucao.summerframework.trie.configuration.property.impl.OriginProperty;
import top.chitucao.summerframework.trie.node.NodeType;
import top.chitucao.summerframework.trie.query.Aggregations;
import top.chitucao.summerframework.trie.query.Criteria;

/**
 * JMH 查询/计数基准：量化 T2(dfsSearch回溯，看分配率) 与 T4(count、getSize O(1)) 的收益。
 * <p>
 * 另含 <b>P1 常规查询基线</b>（方法名前缀 {@code p1_}）：为"levelSearch/childrenAnyMatch 去 Stream churn"改造提供
 * 改造前后对照。关注 GCProfiler 的 {@code gc.alloc.rate.norm}(B/op)——
 * 用以判断查询热路径的分配大头到底是 <i>Stream churn</i>（levelSearch flatMap / childrenAnyMatch 递归的 Stream 中间对象）
 * 还是 <i>每层 newChildren 拷贝命中项</i>（searchAndAgg→Operate.query，对应 T3 中间层免拷贝）。
 * 若 Stream churn 占比小，则应优先投 T3 而非仅去 Stream。
 * <p>
 * 运行：mvn test -Dtest=TrieBenchmark#runJmh （在 runJmh 里用 OptionsBuilder 启动，含 GC 分配分析器）；
 * 仅跑 P1 基线：mvn test -Dtest=TrieBenchmark#runJmh -Djmh.include=p1_
 *
 * @author 资深全栈架构师
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class TrieBenchmark {

    /** 数据规模 */
    private static final int     N = 100_000;

    private static final String[] CABINS = { "ECON", "BIZ", "FIRST", "PECON" };

    public static class Flight {
        private final String cabinClass;
        private final int    depDate;
        private final int    price;
        private final String depCity;
        private final String arrCity;
        private final long   id;

        public Flight(String cabinClass, int depDate, int price, String depCity, String arrCity, long id) {
            this.cabinClass = cabinClass;
            this.depDate = depDate;
            this.price = price;
            this.depCity = depCity;
            this.arrCity = arrCity;
            this.id = id;
        }

        public String getCabinClass() {
            return cabinClass;
        }

        public int getDepDate() {
            return depDate;
        }

        public int getPrice() {
            return price;
        }

        public String getDepCity() {
            return depCity;
        }

        public String getArrCity() {
            return arrCity;
        }

        public long getId() {
            return id;
        }
    }

    /** size 可靠的树(纯插入) → getSize 走 O(1) */
    private Trie<Flight> trie;

    /** 被 fastErase 弄脏的树 → getSize 退化为整树遍历 O(n)，用于对照 */
    private Trie<Flight> dirtyTrie;

    /** 哈希层改用 ADAPTIVE_HASH 的树，用于对照查询速度 */
    private Trie<Flight> adaptiveTrie;

    /** 代表性查询：固定出发城市 + 价格区间 + 舱等 */
    private Criteria     query;

    /** 宽查询：仅按 cabinClass 过滤，命中结果集很大(约 N/4)，用于演示 limit 下推收益 */
    private Criteria     broadQuery;

    /** 宽查询 + limit=10：只取前 10 条，验证“命中即停、不全量物化” */
    private Criteria     broadQueryLimit10;

    /** P1基线·childrenAnyMatch 宽版：过滤条件只在最深的 arrCity 层，展示浅层 depDate → 每个展示键都递归下钻判断后代命中，Stream churn 最重 */
    private Criteria     anyMatchBroadQuery;

    static Trie<Flight> buildTrie() {
        return buildTrie(NodeType.CONCURRENT_HASH_MAP, false);
    }

    static Trie<Flight> buildTrie(NodeType hashType) {
        return buildTrie(hashType, false);
    }

    static Trie<Flight> buildTrie(NodeType hashType, boolean arrayDict) {
        Configuration cfg = new Configuration();

        OriginProperty<Flight, String> cabin = new OriginProperty<>("cabinClass", hashType);
        cabin.setObject2FieldMapper(Flight::getCabinClass);
        cfg.addProperty(cabin);

        OriginProperty<Flight, Integer> depDate = new OriginProperty<>("depDate", NodeType.CONCURRENT_SKIP_LIST_MAP);
        depDate.setObject2FieldMapper(Flight::getDepDate);
        cfg.addProperty(depDate);

        OriginProperty<Flight, Integer> price = new OriginProperty<>("price", NodeType.CONCURRENT_SKIP_LIST_MAP);
        price.setObject2FieldMapper(Flight::getPrice);
        cfg.addProperty(price);

        OriginProperty<Flight, String> depCity = new OriginProperty<>("depCity", hashType);
        depCity.setObject2FieldMapper(Flight::getDepCity);
        cfg.addProperty(depCity);

        OriginProperty<Flight, String> arrCity = new OriginProperty<>("arrCity", hashType);
        arrCity.setObject2FieldMapper(Flight::getArrCity);
        cfg.addProperty(arrCity);

        AutoMappingProperty<Flight, Flight> data = arrayDict
            ? new AutoMappingProperty<>("data", top.chitucao.summerframework.trie.configuration.property.DictKeyType.LONG, hashType,
                new top.chitucao.summerframework.trie.dict.ArrayDict<>())
            : new AutoMappingProperty<>("data", top.chitucao.summerframework.trie.configuration.property.DictKeyType.LONG, hashType);
        data.setObject2FieldMapper(Function.identity());
        cfg.addProperty(data);

        Trie<Flight> t = new MapTrie<>(cfg);
        for (int i = 0; i < N; i++) {
            t.insert(new Flight(CABINS[i % CABINS.length], 20260101 + (i % 60), 100 + (i % 500), "C" + (i % 50), "A" + ((i / 50) % 50), i));
        }
        return t;
    }

    static Criteria buildQuery() {
        return new Criteria().and("cabinClass").eq("ECON").and("price").between(100, 300).and("depCity").eq("C0");
    }

    @Setup
    public void setup() {
        trie = buildTrie();
        dirtyTrie = buildTrie();
        // fastErase 会把 sizeReliable 置为 false（即使没删到东西），使 dirtyTrie 的 getSize 退化为遍历
        dirtyTrie.erase(new Criteria().and("cabinClass").eq("__NONE__"));
        adaptiveTrie = buildTrie(NodeType.ADAPTIVE_HASH);
        query = buildQuery();
        broadQuery = new Criteria().and("cabinClass").eq("ECON");
        broadQueryLimit10 = new Criteria().and("cabinClass").eq("ECON").limit(10);
        anyMatchBroadQuery = new Criteria().and("arrCity").eq("A49");
    }

    /** T2 验证点：分组树查询(看 GC 分配率，回溯后应显著低于"每节点复制路径") */
    @Benchmark
    public Object treeSearch_groupByArrCity() {
        return trie.treeSearch(query, new Aggregations(), "arrCity", "data");
    }

    /** 扁平数据查询 */
    @Benchmark
    public List<Flight> dataSearch() {
        return trie.dataSearch(query);
    }

    /** limit 下推：宽查询全量物化（对照，约 N/4 条结果） */
    @Benchmark
    public List<Flight> dataSearch_broad_full() {
        return trie.dataSearch(broadQuery);
    }

    /** limit 下推：宽查询 limit=10，命中即停，避免全量物化 */
    @Benchmark
    public List<Flight> dataSearch_broad_limit10() {
        return trie.dataSearch(broadQueryLimit10);
    }

    /** limit 下推：分组树宽查询全量（对照） */
    @Benchmark
    public Object treeSearch_broad_full() {
        return trie.treeSearch(broadQuery, new Aggregations(), "arrCity", "data");
    }

    /** limit 下推：分组树宽查询 limit=10，命中即停 */
    @Benchmark
    public Object treeSearch_broad_limit10() {
        return trie.treeSearch(broadQueryLimit10, new Aggregations(), "arrCity", "data");
    }

    /** T4：count(Criteria) 不物化对象 */
    @Benchmark
    public int count_T4() {
        return trie.count(query);
    }

    /** 对照基线：通过物化数据再取 size 来计数 */
    @Benchmark
    public int dataSearchSize_baseline() {
        return trie.dataSearch(query).size();
    }

    /** T4：getSize 可靠时 O(1) */
    @Benchmark
    public int getSize_O1_reliable() {
        return trie.getSize();
    }

    /** 对照：getSize 退化为 O(n) 整树遍历 */
    @Benchmark
    public int getSize_On_traversal() {
        return dirtyTrie.getSize();
    }

    /** C1对照：哈希层用 ADAPTIVE_HASH 的分组树查询(看与 CHM 版的速度差) */
    @Benchmark
    public Object treeSearch_adaptive() {
        return adaptiveTrie.treeSearch(query, new Aggregations(), "arrCity", "data");
    }

    /** C1对照：哈希层用 ADAPTIVE_HASH 的 count */
    @Benchmark
    public int count_adaptive() {
        return adaptiveTrie.count(query);
    }

    /** C1对照：哈希层用 ADAPTIVE_HASH 的扁平查询 */
    @Benchmark
    public List<Flight> dataSearch_adaptive() {
        return adaptiveTrie.dataSearch(query);
    }

    //---------------------------------------- P1 常规查询基线（去 Stream churn 前的对照） ----------------------------------------

    /**
     * P1基线·levelSearch 直返分支(3.1)：展示 arrCity 处于最深过滤条件(depCity)之下/同层，
     * 无后续过滤 → 不触发 childrenAnyMatch，只走 levelSearch 主体的 Stream.flatMap 逐层收窄 + 末层取 keySet。
     * 量这条可看 levelSearch 主循环本身的 Stream 开销 + 每层 newChildren 拷贝。
     */
    @Benchmark
    public List<String> p1_propertySearch_simple() {
        return trie.propertySearch(query, "arrCity");
    }

    /**
     * P1基线·childrenAnyMatch 分支(3.2) 窄版：展示顶层 cabinClass，过滤条件落在更深的 price+depCity，
     * 展示层 cabinClass.eq("ECON") 只剩 1 个键 → 触发 1 次 childrenAnyMatch 递归(depDate→price→depCity)，命中即短路。
     */
    @Benchmark
    public List<String> p1_propertySearch_anyMatch_narrow() {
        return trie.propertySearch(query, "cabinClass");
    }

    /**
     * P1基线·childrenAnyMatch 分支(3.2) 宽版【Stream churn 重灾区】：展示浅层 depDate(约60个键)，
     * 过滤条件只在最深的 arrCity → 每个 depDate 键都要 childrenAnyMatch 递归下钻 price→depCity→arrCity，
     * 每节点新建一组 Stream(of/flatMap/anyMatch) 中间对象。这是 P1 去 Stream 收益最该出现的地方。
     */
    @Benchmark
    public List<Integer> p1_propertySearch_anyMatch_broad() {
        return trie.propertySearch(anyMatchBroadQuery, "depDate");
    }

    /**
     * 在 IDE/maven 里直接跑基准（含 GC 分配分析器）。
     * -1.默认跑全部；可用 -Djmh.include=&lt;正则&gt; 聚焦部分基准，例如 -Djmh.include=broad 只跑 limit 下推对照。
     */
    @Test
    public void runJmh() throws RunnerException {
        String include = System.getProperty("jmh.include", TrieBenchmark.class.getSimpleName());
        Options opt = new OptionsBuilder().include(include).addProfiler(GCProfiler.class).build();
        new Runner(opt).run();
    }
}
