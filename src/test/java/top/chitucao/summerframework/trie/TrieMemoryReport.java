package top.chitucao.summerframework.trie;

import java.util.function.Function;

import org.junit.Test;
import org.openjdk.jol.info.GraphLayout;

import top.chitucao.summerframework.trie.configuration.Configuration;
import top.chitucao.summerframework.trie.configuration.property.impl.OneWayMappingProperty;
import top.chitucao.summerframework.trie.configuration.property.impl.OriginProperty;
import top.chitucao.summerframework.trie.dict.HashMapDict;
import top.chitucao.summerframework.trie.node.NodeType;

/**
 * JOL 内存占用报告：量化 trie 的整体堆占用与各类对象分布，为 C2(去装箱)/C3(String复用)/T7(ART节点) 的内存决策提供数据。
 * <p>
 * 运行：mvn test -Dtest=TrieMemoryReport#report
 *
 * @author 资深全栈架构师
 */
public class TrieMemoryReport {

    @Test
    public void report() {
        int n = 100_000;

        Trie<TrieBenchmark.Flight> chm = TrieBenchmark.buildTrie(NodeType.CONCURRENT_HASH_MAP, false);
        Trie<TrieBenchmark.Flight> adaptive = TrieBenchmark.buildTrie(NodeType.ADAPTIVE_HASH, false);
        Trie<TrieBenchmark.Flight> adaptiveArr = TrieBenchmark.buildTrie(NodeType.ADAPTIVE_HASH, true);

        long chmSize = GraphLayout.parseInstance(chm).totalSize();
        long adSize = GraphLayout.parseInstance(adaptive).totalSize();
        GraphLayout adArrLayout = GraphLayout.parseInstance(adaptiveArr);
        long adArrSize = adArrLayout.totalSize();

        System.out.println();
        System.out.println("==================== summer-trie 内存报告 (N=" + n + ") ====================");
        System.out.printf("CONCURRENT_HASH_MAP            : %,d bytes (%d MB), %d B/record%n", chmSize, chmSize / 1024 / 1024, chmSize / n);
        System.out.printf("ADAPTIVE_HASH                 : %,d bytes (%d MB), %d B/record%n", adSize, adSize / 1024 / 1024, adSize / n);
        System.out.printf("ADAPTIVE_HASH + ArrayDict     : %,d bytes (%d MB), %d B/record%n", adArrSize, adArrSize / 1024 / 1024, adArrSize / n);
        System.out.printf("ArrayDict 相对 ADAPTIVE 再省   : %,d bytes (%.1f%%)%n", adSize - adArrSize, 100.0 * (adSize - adArrSize) / adSize);
        System.out.printf("全栈相对 CHM 共省             : %,d bytes (%.1f%%)%n", chmSize - adArrSize, 100.0 * (chmSize - adArrSize) / chmSize);
        System.out.println();
        System.out.println("---- ADAPTIVE_HASH + ArrayDict 对象分布(类直方图) ----");
        System.out.println(adArrLayout.toFootprint());
        System.out.println("======================================================================");
    }

    public static class Rec {
        private final String region;
        private final long   id;

        public Rec(String region, long id) {
            this.region = region;
            this.id = id;
        }

        public String getRegion() {
            return region;
        }

        public long getId() {
            return id;
        }
    }

    private Trie<Rec> buildOneWay(int n, boolean withIndexer) {
        Configuration cfg = new Configuration();
        OriginProperty<Rec, String> region = new OriginProperty<>("region", NodeType.ADAPTIVE_HASH);
        region.setObject2FieldMapper(Rec::getRegion);
        cfg.addProperty(region);

        OneWayMappingProperty<Rec, Rec, Long> data = withIndexer
            ? new OneWayMappingProperty<>("data", NodeType.ADAPTIVE_HASH, new HashMapDict<>(true, false))
            : new OneWayMappingProperty<>("data", NodeType.ADAPTIVE_HASH);
        data.setObject2FieldMapper(Function.identity());
        data.setField2NodeKeyMapper(Rec::getId);
        cfg.addProperty(data);

        Trie<Rec> t = new MapTrie<>(cfg);
        for (int i = 0; i < n; i++) {
            t.insert(new Rec("R" + (i % 50), i));
        }
        return t;
    }

    /** 量化 OneWayMapping 关 indexer 的内存收益（数据叶子=OneWay） */
    @Test
    public void reportOneWayIndexer() {
        int n = 100_000;
        long withIdx = GraphLayout.parseInstance(buildOneWay(n, true)).totalSize();
        long noIdx = GraphLayout.parseInstance(buildOneWay(n, false)).totalSize();
        System.out.println();
        System.out.println("======== OneWayMapping 数据叶子 indexer 收益 (N=" + n + ") ========");
        System.out.printf("保留 indexer : %,d bytes (%d MB), %d B/record%n", withIdx, withIdx / 1024 / 1024, withIdx / n);
        System.out.printf("关闭 indexer : %,d bytes (%d MB), %d B/record%n", noIdx, noIdx / 1024 / 1024, noIdx / n);
        System.out.printf("节省         : %,d bytes (%.1f%%)%n", withIdx - noIdx, 100.0 * (withIdx - noIdx) / withIdx);
        System.out.println("================================================================");
    }
}
