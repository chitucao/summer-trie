package top.chitucao.summerframework.trie;

import org.junit.Test;
import org.openjdk.jol.info.GraphLayout;

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
}
