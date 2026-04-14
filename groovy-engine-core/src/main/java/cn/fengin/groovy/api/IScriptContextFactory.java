package cn.fengin.groovy.api;

import cn.fengin.groovy.model.MethodInfo;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 脚本上下文工厂接口
 * <p>
 * 各业务模块实现此接口，定义 Groovy 脚本运行时可访问的所有上下文内容，包括：
 * <ul>
 *   <li><b>请求参数</b> — 前端传入的业务参数（params）</li>
 *   <li><b>用户信息</b> — 当前登录用户的 userId 等</li>
 *   <li><b>业务 Bean</b> — 核心查询服务、计算服务等</li>
 *   <li><b>公共工具</b> — Redis、Logger、getBean 等基础设施</li>
 *   <li><b>常量/配置</b> — 按需注入的业务常量</li>
 * </ul>
 * <p>
 * 注意：以下内容由框架（{@link cn.fengin.groovy.engine.ScriptExecutionService}）
 * 自动注入，<b>无需</b>在本方法中处理：
 * <ul>
 *   <li>{@code t} — ScriptTracer 实例，脚本中调用 {@code t.log("xxx")} 输出追踪日志</li>
 *   <li>{@code _trace} / {@code _track} — 框架自动组装到返回结果中</li>
 * </ul>
 *
 * <h3>实现示例</h3>
 * <pre>{@code
 * @Component
 * public class MyScriptContextFactory implements IScriptContextFactory {
 *
 *     @Resource
 *     private MyQueryService queryService;
 *
 *     @Override
 *     public Map<String, Object> buildContext(Map<String, Object> params) {
 *         Map<String, Object> ctx = new HashMap<>();
 *         ctx.put("params", params != null ? params : Collections.emptyMap());
 *         ctx.put("queryService", queryService);
 *         ctx.put("log", LoggerFactory.getLogger("GroovyScript"));
 *         return ctx;
 *     }
 * }
 * }</pre>
 *
 * @author 凌封 (https://aibook.ren)
 */
public interface IScriptContextFactory {

    /**
     * 构建脚本执行上下文
     * <p>
     * 返回的 Map 将作为 Groovy 脚本的 {@code context} 变量，脚本内可直接通过
     * {@code context.key} 访问所有注入的内容。
     *
     * @param params 合并后的请求参数（含 query params + body + headers），不为 null
     * @return 脚本可访问的上下文 Map，不得返回 null
     */
    Map<String, Object> buildContext(Map<String, Object> params);

    /**
     * 返回可用 Bean 的方法签名，用于 Web IDE 代码补全。
     * <p>
     * key 为 Bean 在 context 中的名称（如 "queryService"），value 为该 Bean 的公开方法列表。
     * 仅在此处注册的 Bean 才会在 track 模式下被框架代理追踪。
     * <p>
     * 默认返回空 Map，各业务模块按需覆盖。
     */
    default Map<String, List<MethodInfo>> getCompletionData() {
        return Collections.emptyMap();
    }
}
