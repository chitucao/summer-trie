package top.chitucao.summerframework.trie.node;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 叶子(数据)节点：表示一条记录的终点，没有子节点。
 * <p>
 * 与普通 MapNode 不同，它<b>不分配任何 children 容器</b>（普通实现里每个叶子都挂着一个空 ConcurrentHashMap，
 * 在大数据量下是显著的内存浪费——JOL 实测约占总内存 15%）。叶子的 subtreeSize 恒为 1（它本身就是一条记录）。
 *
 * @param <K> 节点值类型
 * @author 资深全栈架构师
 */
public class LeafNode<K> implements Node<K> {

    @Override
    public Map<K, Node<K>> children() {
        return Collections.emptyMap();
    }

    @Override
    public void setChildren(Map<K, Node<K>> children) {
        throw new UnsupportedOperationException("LeafNode has no children");
    }

    @Override
    public int childSize() {
        return 0;
    }

    @Override
    public Set<K> childKeySet() {
        return Collections.emptySet();
    }

    @Override
    public Node<K> child(K key) {
        return null;
    }

    @Override
    public Node<K> putChild(K key, Node<K> child) {
        throw new UnsupportedOperationException("LeafNode cannot have children");
    }

    @Override
    public Node<K> putChild(K key, Supplier<Node<K>> childSupplier) {
        throw new UnsupportedOperationException("LeafNode cannot have children");
    }

    @Override
    public void removeChild(K key) {
        // 叶子无子节点，忽略
    }

    @Override
    public int subtreeSize() {
        // 叶子恒为 1 条记录
        return 1;
    }

    @Override
    public void addSubtreeSize(int delta) {
        // 叶子恒为 1，无需维护
    }
}
