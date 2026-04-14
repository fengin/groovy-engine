package cn.fengin.groovy.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Groovy 业务脚本信息（纯 POJO，无 ORM 注解）
 * <p>
 * 对应数据库表 {@code sys_groovy_script}，但本类不包含任何持久化框架依赖。
 * 存储层实现（如 JdbcScriptStorage）内部使用 ORM 实体与本类互转。
 *
 * @author 凌封 (https://aibook.ren)
 */
public class GroovyScript implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 业务编码（路由标识） */
    private String bizCode;

    /** 接口名称 */
    private String name;

    /** Groovy 脚本源码 */
    private String scriptContent;

    /** 版本号 */
    private Integer version;

    /** 1=启用 0=禁用 */
    private Integer status;

    /** 项目编码（用于按项目导入/导出） */
    private String projectCode;

    /** 分类标签 */
    private String category;

    /** 备注 */
    private String remark;

    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;

    public GroovyScript() {
    }

    // ==================== Getter / Setter ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBizCode() {
        return bizCode;
    }

    public void setBizCode(String bizCode) {
        this.bizCode = bizCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getScriptContent() {
        return scriptContent;
    }

    public void setScriptContent(String scriptContent) {
        this.scriptContent = scriptContent;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getProjectCode() {
        return projectCode;
    }

    public void setProjectCode(String projectCode) {
        this.projectCode = projectCode;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
