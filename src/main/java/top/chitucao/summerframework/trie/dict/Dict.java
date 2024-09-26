package top.chitucao.summerframework.trie.dict;

import java.util.Map;
import java.util.Set;

/**
 * 字段值字典
 * -1.如果字段本身就是数字，也可以考虑不用字典，节省空间；
 * -2.可以通过这个对象直接拿到某个字段的所有有效值，比如限制前端下拉框的有效值；
 * -3.为什么要存在这样一个字典？     比起在节点中直接存字段值本身，将字段映射成数字可能节省空间一点；
 * -4.为什么字典key用number类型？   实现简单，hashCode 和 equals高效一点，也有一部分原因是现有实现时偷懒了，是可以将dictKey抽象一下的；
 * -5.针对某个字段的字典，也可以被多个前缀树共享，进一步节省空间；
 * -6.如果字段有枚举，可以直接用现有的枚举实现这个字典；
 *
 * @author chitucao(zhonggang.zhu)
 * @version Id: Dict.java, v 0.1 2024-09-05 15:29 chitucao Exp $$
 */
public interface Dict<R> {

    /**
     * 字典数量
     *
     * @return 字典数量
     */
    int getSize();

    /**
     * 所有字典值
     *
     * @return 所有字典值
     */
    Set<R> dictValues();

    /**
     * 所有字典键值
     *
     * @return  所有字典键值
     */
    Map<Number, R> dictAll();

    /**
     * 查询字段值对应的字典key
     *
     * @param r 字段值
     * @return 字典key
     */
    Number getDictKey(R r);

    /**
     * 查询字典key对应的字段值
     *
     * @param dictKey 字典key
     * @return 字段值
     */
    R getDictValue(Number dictKey);

    /**
     * 新增一个字典
     *
     * @param dictKey   字典key
     * @param dictValue 字段值
     */
    void putDict(Number dictKey, R dictValue);

    /**
     * 新增一个字典
     *
     * @param dictKey   字典key
     * @param dictValue 字段值
     */
    void putDictObj(Number dictKey, Object dictValue);

    /**
     * 是否包含某个字典key
     *
     * @param dictKey 字典key
     * @return 是否包含某个字典key
     */
    boolean containsDictKey(Number dictKey);

    /**
     * 是否包含某个字段
     *
     * @param r 字段
     * @return 是否包含某个字段
     */
    boolean containsDictValue(R r);

    /**
     * 减少一个字典key的数量
     * 
     * @param dictKey   字典key
     * @param count     减少的数量
     */
    void decrDictCount(Number dictKey, int count);

    /**
     * 删除一个字典key
     *
     * @param dictKey   字典key
     */
    void removeDictKey(Number dictKey);

}