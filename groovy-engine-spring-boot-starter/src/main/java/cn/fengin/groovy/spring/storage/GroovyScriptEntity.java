package cn.fengin.groovy.spring.storage;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Groovy 脚本数据库实体（MyBatis-Plus）
 *
 * @author 凌封 (https://aibook.ren)
 */
@Data
@TableName("sys_groovy_script")
public class GroovyScriptEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
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

    /** 项目编码 */
    private String projectCode;

    /** 分类标签 */
    private String category;

    /** 备注 */
    private String remark;

    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
}
