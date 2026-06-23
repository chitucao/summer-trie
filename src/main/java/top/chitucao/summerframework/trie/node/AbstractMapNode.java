package top.chitucao.summerframework.trie.node;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.function.Supplier;

/**
 * map实现的trie节点抽象
 * @param <K>   节点值类型
 *
 * @author chitucao
 */
public abstract class AbstractMapNode<K> implements Node<K> {

    /**
     * 子节点映射表
     */
    protected Map<K, Node<K>>                                       children;

    /**
     * 子树数据(记录)总数：叶子为1，内部节点为子树数据之和；由 insert 与精确删除沿路径原子维护
     * -1.用 volatile int + AtomicIntegerFieldUpdater 而非 AtomicInteger，避免每个节点多一个对象的内存开销；
     */
    protected volatile int                                          subtreeSize;

    private static final AtomicIntegerFieldUpdater<AbstractMapNode> SUBTREE_SIZE_UPDATER = AtomicIntegerFieldUpdater.newUpdater(AbstractMapNode.class, "subtreeSize");

    /**
     * 子节点映射表
     *
     * @return 子节点映射表
     */
    @Override
    public Map<K, Node<K>> children() {
        return children;
    }

    /**
     * 子节点数量
     *
     * @return 子节点数量
     */
    @Override
    public int childSize() {
        return children.size();
    }

    @Override
    public int subtreeSize() {
        return subtreeSize;
    }

    @Override
    public void addSubtreeSize(int delta) {
        SUBTREE_SIZE_UPDATER.addAndGet(this, delta);
    }

    /**
     * 子节点key集合
     *
     * @return 子节点key集合
     */
    @Override
    public Set<K> childKeySet() {
        return children.keySet();
    }

    /**
     * 子节点
     *
     * @return  子节点key
     */
    @Override
    public Node<K> child(K key) {
        return children.get(key);
    }

    /**
     * 添加子节点
     *
     * @param key   子节点key
     * @param child 子节点
     */
    @Override
    public Node<K> putChild(K key, Node<K> child) {
        // 原子化：在并发节点(ConcurrentHashMap/ConcurrentSkipListMap)下，computeIfAbsent 保证同一 key 只保留一个子节点，
        // 杜绝原先 get 后 put 的 check-then-act 竞态（并发插同 key 会各自 new 节点互相覆盖、丢整棵子树）
        return children.computeIfAbsent(key, k -> child);
    }

    /**
     * 添加子节点
     *
     * @param key           子节点key
     * @param childSupplier 子节点提供者
     * @return 添加子节点
     */
    @Override
    public Node<K> putChild(K key, Supplier<Node<K>> childSupplier) {
        // 原子化：仅在 key 不存在时才通过 supplier 创建子节点，避免并发各自创建节点互相覆盖
        return children.computeIfAbsent(key, k -> childSupplier.get());
    }

    /**
     * 删除子节点
     *
     * @param key 字典key
     */
    @Override
    public void removeChild(K key) {
        children.remove(key);
    }
}