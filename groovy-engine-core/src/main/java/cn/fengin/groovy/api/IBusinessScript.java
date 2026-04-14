package cn.fengin.groovy.api;

import java.util.Map;

/**
 * Groovy 业务脚本标准契约。
 * <p>
 * 所有存储在 sys_groovy_script 中的 Groovy 脚本类必须实现此接口。
 *
 * @author 凌封 (https://aibook.ren)
 */
public interface IBusinessScript {
    /**
     * 执行业务逻辑
     *
     * @param context 执行上下文，包含：
     *                - params: Map 前端传入的请求参数
     *                - 预注册的核心 Java Bean 引用
     *                - getBean: BeanAccessor 按需获取其他 Bean
     *                - t: ScriptTracer 追踪工具
     *                - log: Logger
     * @return 业务结果，将被 JSON 序列化后返回给前端
     */
    Object execute(Map<String, Object> context);
}
