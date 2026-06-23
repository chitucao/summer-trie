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
 * 运行：mvn test -Dtest=TrieBenchmark#runJmh （在 runJmh 里用 OptionsBuilder 启动，含 GC 分配分析器）
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

    /** 在 IDE/maven 里直接跑全部基准（含 GC 分配分析器） */
    @Test
    public void runJmh() throws RunnerException {
        Options opt = new OptionsBuilder().include(TrieBenchmark.class.getSimpleName()).addProfiler(GCProfiler.class).build();
        new Runner(opt).run();
    }
}
