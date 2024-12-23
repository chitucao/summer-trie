//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package top.chitucao.summerframework.trie.utils;

import java.io.Serializable;
import java.util.Objects;

/**
 * 键值对
 *
 * @author chitucao
 */
public class Pair<K, V> implements Serializable {
    private static final long serialVersionUID = 1L;
    protected K               key;
    protected V               value;

    public static <K, V> Pair<K, V> of(K key, V value) {
        return new Pair<K, V>(key, value);
    }

    public Pair() {
    }

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    public K getKey() {
        return this.key;
    }

    public void setKey(K key) {
        this.key = key;
    }

    public V getValue() {
        return this.value;
    }

    public void setValue(V value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "Pair [key=" + this.key + ", value=" + this.value + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof Pair)) {
            return false;
        } else {
            //noinspection rawtypes
            Pair<?, ?> pair = (Pair) o;
            return Objects.equals(this.getKey(), pair.getKey()) && Objects.equals(this.getValue(), pair.getValue());
        }
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.key) ^ Objects.hashCode(this.value);
    }

}
