package top.chitucao.summerframework.trie.operation;

/**
 * 操作类型
 * -1.这里提供了一些常用的操作，提供的操作类型主要是考虑两个方面：
 *  -1.1 trie实现起来效率比较高；
 *  -1.2 业务常用的；
 * -2.当然也可以支持注册自定义的操作类型（$xxx这样），这个后面会支持；
 *
 * @author chitucao(zhonggang.zhu)
 * @version Id: Operation.java, v 0.1 2025-08-11 10:53 chitucao Exp $$
 */
public enum Operation {

                       /** 等于 */
                       EQ("$eq"),
                       /** 不等于 */
                       NE("$ne"),

                       /** 大于 */
                       GT("$gt"),
                       /** 大于等于 */
                       GTE("$gte"),

                       /** 小于 */
                       LT("$lt"),
                       /** 小于等于 */
                       LTE("$lte"),

                       /** 区间 左闭右闭 */
                       BETWEEN("$between"),

                       /** 包含 */
                       IN("$in"),
                       /** 不包含 */
                       NIN("$nin");

    private final String value;

    Operation(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}