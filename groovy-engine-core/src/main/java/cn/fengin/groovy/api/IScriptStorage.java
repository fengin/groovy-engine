package cn.fengin.groovy.api;

import cn.fengin.groovy.model.GroovyScript;

import java.util.List;

/**
 * 脚本存储抽象接口
 * <p>
 * 引擎核心通过此接口访问脚本数据，不直接依赖任何 ORM 框架。
 * 框架提供基于 MyBatis-Plus 的默认实现（JdbcScriptStorage），
 * 业务方也可以自定义实现（如 Redis 存储、文件存储等）。
 *
 * @author 凌封 (https://aibook.ren)
 */
public interface IScriptStorage {

    // ==================== 引擎执行用 ====================

    /**
     * 根据 bizCode 获取已启用的脚本
     *
     * @param bizCode 业务编码
     * @return 脚本信息，不存在或未启用返回 null
     */
    GroovyScript getByBizCode(String bizCode);

    /**
     * 获取所有已启用的脚本（用于预加载）
     */
    List<GroovyScript> listEnabledScripts();

    // ==================== 管理 API 用 ====================

    /**
     * 根据 ID 获取脚本详情（含 scriptContent）
     */
    GroovyScript getById(Long id);

    /**
     * 查询脚本列表（支持按分类和项目编码过滤）
     *
     * @param category    分类标签，null 表示不过滤
     * @param projectCode 项目编码，null 表示不过滤
     * @return 脚本列表（不含 scriptContent 以节省带宽）
     */
    List<GroovyScript> listScripts(String category, String projectCode);

    /**
     * 保存新脚本
     */
    void save(GroovyScript script);

    /**
     * 更新脚本
     */
    void updateById(GroovyScript script);

    /**
     * 删除脚本
     */
    void removeById(Long id);
}
