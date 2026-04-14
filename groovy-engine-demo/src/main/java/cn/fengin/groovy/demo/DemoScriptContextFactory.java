package cn.fengin.groovy.demo;

import cn.fengin.groovy.api.IScriptContextFactory;
import cn.fengin.groovy.engine.BeanAccessor;
import cn.fengin.groovy.model.MethodInfo;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Demo 脚本上下文工厂
 * <p>
 * 演示如何实现 {@link IScriptContextFactory}，
 * 注入业务 Bean 到 Groovy 脚本执行上下文。
 *
 * @author 凌封 (https://aibook.ren)
 */
@Component
public class DemoScriptContextFactory implements IScriptContextFactory {

    @Resource
    private ApplicationContext applicationContext;

    private final DemoService demoService = new DemoService();

    private static final Set<String> ALLOWED_PACKAGES = new HashSet<>(Arrays.asList(
            "cn.fengin.groovy.demo"
    ));

    @Override
    public Map<String, Object> buildContext(Map<String, Object> params) {
        Map<String, Object> ctx = new HashMap<>();

        // ① 请求参数
        ctx.put("params", params != null ? params : Collections.emptyMap());

        // ② 核心业务 Bean
        ctx.put("demoService", demoService);

        // ③ 灵活获取 Bean（按需）
        ctx.put("getBean", new BeanAccessor(applicationContext, ALLOWED_PACKAGES));

        // ④ 通用工具
        ctx.put("log", LoggerFactory.getLogger("GroovyScript"));

        return ctx;
    }

    @Override
    public Map<String, List<MethodInfo>> getCompletionData() {
        Map<String, List<MethodInfo>> data = new LinkedHashMap<>();
        data.put("demoService", extractMethods(DemoService.class));
        return data;
    }

    private List<MethodInfo> extractMethods(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> java.lang.reflect.Modifier.isPublic(m.getModifiers()))
                .map(m -> {
                    String params = Arrays.stream(m.getParameters())
                            .map(p -> p.getType().getSimpleName() + " " + p.getName())
                            .collect(Collectors.joining(", "));
                    return new MethodInfo(m.getName(), params, m.getReturnType().getSimpleName());
                })
                .collect(Collectors.toList());
    }
}
