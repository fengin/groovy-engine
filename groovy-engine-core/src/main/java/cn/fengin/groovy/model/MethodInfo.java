package cn.fengin.groovy.model;

/**
 * Bean 方法签名信息，用于 Web IDE 代码补全
 *
 * @author 凌封 (https://aibook.ren)
 */
public class MethodInfo {

    /** 方法名 */
    private String name;
    /** 参数列表描述，如 "Long configId, List<Long> itemIds" */
    private String params;
    /** 返回值类型，如 "BigDecimal" */
    private String returnType;

    public MethodInfo(String name, String params, String returnType) {
        this.name = name;
        this.params = params;
        this.returnType = returnType;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getParams() { return params; }
    public void setParams(String params) { this.params = params; }
    public String getReturnType() { return returnType; }
    public void setReturnType(String returnType) { this.returnType = returnType; }
}
