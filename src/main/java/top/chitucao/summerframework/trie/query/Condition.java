package top.chitucao.summerframework.trie.query;

/**
 * 查询条件
 *
 * @author chitucao
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