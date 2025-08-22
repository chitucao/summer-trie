package top.chitucao.summerframework.trie.query;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * 树节点，用于树查询的展示
 * @author chitucao(zhonggang.zhu)
 * @version Id: TreeNode.java, v 0.1 2025-08-20 18:06 chitucao Exp $$
 */
public class TreeNode implements Serializable {

    /** 节点名称 */
    private String                          name;

    /** 子节点 */
    private List<TreeNode>                  children;

    @JsonIgnore
    private transient Map<String, TreeNode> childMap;

    public TreeNode(String name) {
        this.name = name;
        children = new ArrayList<>();
        childMap = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<TreeNode> getChildren() {
        return children;
    }

    public void setChildren(List<TreeNode> children) {
        this.children = children;
    }

    public Map<String, TreeNode> getChildMap() {
        return childMap;
    }

    public void setChildMap(Map<String, TreeNode> childMap) {
        this.childMap = childMap;
    }
}