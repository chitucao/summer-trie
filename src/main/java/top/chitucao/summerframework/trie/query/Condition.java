package top.chitucao.summerframework.trie.query;

/**
 * 查询条件
 *
 * @author chitucao(zhonggang.zhu)
 * @version Id: Condition.java, v 0.1 2024-08-06 下午4:55 chitucao Exp $$
 */
public enum Condition {
                       /** 等值 */
                       EQUAL,
                       /** 范围 左闭右闭 */
                       BETWEEN,
                       /** 大于等于 */
                       GTE,
                       /** 小于等于 */
                       LTE,
                       /** 包含 */
                       IN,
                       /** 不包含 */
                       NOT_IN
}