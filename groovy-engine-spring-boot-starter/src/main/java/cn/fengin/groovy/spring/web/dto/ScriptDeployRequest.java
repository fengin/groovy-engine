package cn.fengin.groovy.spring.web.dto;

/**
 * 脚本批量部署请求 DTO
 *
 * @author 凌封 (https://aibook.ren)
 */
public class ScriptDeployRequest {

    /** 业务编码 */
    private String bizCode;
    /** 接口名称 */
    private String name;
    /** Groovy 脚本源码 */
    private String scriptContent;
    /** 分类标签 */
    private String category;
    /** 项目编码 */
    private String projectCode;
    /** 备注 */
    private String remark;

    public String getBizCode() { return bizCode; }
    public void setBizCode(String bizCode) { this.bizCode = bizCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getScriptContent() { return scriptContent; }
    public void setScriptContent(String scriptContent) { this.scriptContent = scriptContent; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getProjectCode() { return projectCode; }
    public void setProjectCode(String projectCode) { this.projectCode = projectCode; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
