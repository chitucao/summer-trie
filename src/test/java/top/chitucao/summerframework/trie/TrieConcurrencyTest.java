package top.chitucao.summerframework.trie;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.junit.Test;

import top.chitucao.summerframework.trie.configuration.Configuration;
import top.chitucao.summerframework.trie.configuration.property.DictKeyType;
import top.chitucao.summerframework.trie.configuration.property.impl.AutoMappingProperty;
import top.chitucao.summerframework.trie.configuration.property.impl.OriginProperty;
import top.chitucao.summerframework.trie.node.NodeType;
import top.chitucao.summerframework.trie.query.Criteria;

/**
 * 并发安全单测，验证 T1 的并发加固（A1 putChild 原子化 / A2 计数原子化 / A3 自增key原子分配 / A4 insert 预校验防悬挂路径）。
 * <p>
 * 设计要点：刻意制造高竞争——少量城市值被大量线程并发插入，触发 putChild 的 check-then-act 与 AutoMapping 自增key分配竞态。
 *
 * @author 资深全栈架构师
 */
public class TrieConcurrencyTest {

    /** 自包含测试数据对象（不覆写 equals/hashCode，按引用区分，保证每条记录是独立叶子） */
    public static class Item {
        private final String  city;
        private final Integer price;
        private final long    id;

        public Item(String city, Integer price, long id) {
            this.city = city;
            this.price = price;
            this.id = id;
        }

        public String getCity() {
            return city;
        }

        public Integer getPrice() {
            return price;
        }

        public long getId() {
            return id;
        }
    }

    /** 构建上下文：持有 trie 与 city 属性（用于断言字典状态） */
    private static class Ctx {
        Trie<Item>                        trie;
        AutoMappingProperty<Item, String> cityProp;
    }

    /** 城市 -> AutoMapping(INT, 并发HashMap)；价格 -> Origin(并发跳表)；数据 -> AutoMapping(LONG, 并发HashMap) */
    private Ctx build() {
        Configuration cfg = new Configuration();

        AutoMappingProperty<Item, String> cityProp = new AutoMappingProperty<>("city", DictKeyType.INT, NodeType.CONCURRENT_HASH_MAP);
        cityProp.setObject2FieldMapper(Item::getCity);
        cfg.addProperty(cityProp);

        OriginProperty<Item, Integer> priceProp = new OriginProperty<>("price", NodeType.CONCURRENT_SKIP_LIST_MAP);
        priceProp.setObject2FieldMapper(Item::getPrice);
        cfg.addProperty(priceProp);

        AutoMappingProperty<Item, Item> dataProp = new AutoMappingProperty<>("data", DictKeyType.LONG, NodeType.CONCURRENT_HASH_MAP);
        dataProp.setObject2FieldMapper(Function.identity());
        cfg.addProperty(dataProp);

        Ctx ctx = new Ctx();
        ctx.trie = new MapTrie<>(cfg);
        ctx.cityProp = cityProp;
        return ctx;
    }

    private static final String[] CITIES = { "PEK", "SHA", "CAN", "CTU", "SZX", "HGH", "XIY", "WUH", "CKG", "KMG", "NKG", "TAO" };

    /**
     * A1 + A3：多线程并发插入，验证无丢子树（getSize 准确）、无重复分配自增key（字典大小=去重城市数）、按值查询数量正确。
     */
    @Test
    public void testConcurrentInsert_noLostSubtree_noDuplicateDictKey() throws Exception {
        final int threadCount = 8;
        final int perThread = 2000;
        final int total = threadCount * perThread;

        Ctx ctx = build();
        final Trie<Item> trie = ctx.trie;

        // 预生成数据，便于精确核对每个城市的期望条数
        List<Item> items = new ArrayList<>(total);
        Map<String, Integer> expectByCity = new HashMap<>();
        for (int i = 0; i < total; i++) {
            String city = CITIES[i % CITIES.length];
            items.add(new Item(city, i % 1000, i));
            expectByCity.merge(city, 1, Integer::sum);
        }

        // 并发插入（所有线程抢占同一计数器，城市值高度重叠以制造竞争）
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        final AtomicInteger idx = new AtomicInteger(0);
        List<Callable<Void>> tasks = new ArrayList<>();
        for (int t = 0; t < threadCount; t++) {
            tasks.add(() -> {
                int i;
                while ((i = idx.getAndIncrement()) < total) {
                    trie.insert(items.get(i));
                }
                return null;
            });
        }
        for (Future<Void> f : pool.invokeAll(tasks)) {
            f.get(); // 任何线程异常都会在这里抛出，使测试失败
        }
        pool.shutdown();

        // A1：没有任何子树因 putChild 竞态丢失
        assertEquals("getSize 应等于插入总量（A1：无丢子树）", total, trie.getSize());
        assertEquals("全量数据应可检索", total, trie.dataSearch(new Criteria()).size());

        // A3：同一城市值只分配一个自增key，字典不膨胀
        assertEquals("city 字典 nodeKey 数应等于去重城市数（A3：无重复分配key）", CITIES.length, ctx.cityProp.getDict().size());
        assertEquals("city 字典字段值数应等于去重城市数", CITIES.length, ctx.cityProp.getDict().fieldValues().size());

        // 按城市值查询，数量逐一核对（若 key 被重复分配，这里会查不全）
        for (String city : CITIES) {
            int got = trie.dataSearch(new Criteria().and("city").eq(city)).size();
            assertEquals("城市[" + city + "]按值查询数量应正确", (int) expectByCity.get(city), got);
        }
    }

    /**
     * A4：中间层字段为 null 时，insert 必须在落树前抛异常，不得残留“悬挂路径”（脏字典/脏分支）。
     */
    @Test
    public void testInsertNullMiddleField_noHangingPath() {
        Ctx ctx = build();
        Trie<Item> trie = ctx.trie;

        // 先插入一条正常数据
        trie.insert(new Item("PEK", 100, 1L));
        assertEquals(1, trie.getSize());

        // 再插入一条“城市非空、价格为null”的数据：price 是中间层
        try {
            trie.insert(new Item("GHOST", null, 2L));
            fail("中间层字段为 null 应抛 IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // ok
        }

        // 校验没有残留悬挂路径：GHOST 不应进入字典，也不应出现在树里，size 不变
        assertEquals("失败的插入不应改变数据量", 1, trie.getSize());
        assertNull("GHOST 不应被登记进 city 字典（A4：无脏字典）", ctx.cityProp.getDict().getNodeKey("GHOST"));
        assertTrue("GHOST 不应出现在 city 维度", trie.<String> propertySearch(new Criteria(), "city").stream().noneMatch("GHOST"::equals));

        // 字典/树仍可正常使用：再插一条正常数据应成功
        trie.insert(new Item("SHA", 200, 3L));
        assertEquals(2, trie.getSize());
    }

    /**
     * 并发读写冒烟测试：写线程持续插入的同时读线程持续查询，验证不抛异常（ConcurrentModification 等），最终数据量准确。
     */
    @Test
    public void testConcurrentReadWhileWrite_noException() throws Exception {
        final int writerCount = 6;
        final int perWriter = 2000;
        final int total = writerCount * perWriter;
        final int readerCount = 4;

        Ctx ctx = build();
        final Trie<Item> trie = ctx.trie;

        List<Item> items = new ArrayList<>(total);
        for (int i = 0; i < total; i++) {
            items.add(new Item(CITIES[i % CITIES.length], i % 1000, i));
        }

        final AtomicInteger idx = new AtomicInteger(0);
        final AtomicBoolean writersDone = new AtomicBoolean(false);
        final AtomicReference<Throwable> readerError = new AtomicReference<>();

        ExecutorService pool = Executors.newFixedThreadPool(writerCount + readerCount);
        List<Callable<Void>> tasks = new ArrayList<>();

        // 写线程
        for (int w = 0; w < writerCount; w++) {
            tasks.add(() -> {
                int i;
                while ((i = idx.getAndIncrement()) < total) {
                    trie.insert(items.get(i));
                }
                return null;
            });
        }
        // 读线程（与写并发，持续按城市查询直到写完）
        for (int r = 0; r < readerCount; r++) {
            final int seed = r;
            tasks.add(() -> {
                try {
                    int n = 0;
                    while (!writersDone.get()) {
                        String city = CITIES[(n++ + seed) % CITIES.length];
                        trie.dataSearch(new Criteria().and("city").eq(city));
                    }
                } catch (Throwable e) {
                    readerError.set(e);
                }
                return null;
            });
        }

        List<Future<Void>> futures = new ArrayList<>();
        for (Callable<Void> task : tasks) {
            futures.add(pool.submit(task));
        }
        // 等写线程把数据灌完（前 writerCount 个 future）
        for (int i = 0; i < writerCount; i++) {
            futures.get(i).get();
        }
        writersDone.set(true);
        for (Future<Void> f : futures) {
            f.get();
        }
        pool.shutdown();
        pool.awaitTermination(5, TimeUnit.SECONDS);

        assertNull("并发读期间不应抛异常", readerError.get());
        assertEquals("并发读写后数据量应准确", total, trie.getSize());
        assertEquals("并发读写后全量可检索", total, trie.dataSearch(new Criteria()).size());
    }
}
