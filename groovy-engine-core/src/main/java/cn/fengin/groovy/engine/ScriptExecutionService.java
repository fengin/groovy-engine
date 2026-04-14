package cn.fengin.groovy.engine;

import cn.fengin.groovy.api.IBusinessScript;
import cn.fengin.groovy.api.IScriptContextFactory;
import cn.fengin.groovy.exception.GroovyEngineException;
import cn.fengin.groovy.exception.ScriptExecutionException;
import cn.fengin.groovy.exception.ScriptNotFoundException;
import cn.fengin.groovy.exception.ScriptTimeoutException;
import cn.fengin.groovy.model.BeanCallInfo;
import cn.fengin.groovy.model.MethodInfo;
import cn.fengin.groovy.model.ScriptResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.*;

/**
 * 脚本执行门面 — 统一执行入口
 * <p>
 * 纯 Java 实现，无 Spring 依赖。由 Starter 的 AutoConfiguration 负责实例化和注入。
 *
 * @author 凌封 (https://aibook.ren)
 */
public class ScriptExecutionService {

    private static final Logger log = LoggerFactory.getLogger(ScriptExecutionService.class);
    private static final Logger SCRIPT_LOGGER = LoggerFactory.getLogger("GROOVY_SCRIPT_AUDIT");

    private final ScriptManager scriptManager;
    private final IScriptContextFactory contextFactory;

    /** 超时看门狗线程：到期后中断执行脚本的请求线程 */
    private final ScheduledExecutorService timeoutScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "groovy-timeout-watchdog");
        t.setDaemon(true);
        return t;
    });

    public ScriptExecutionService(ScriptManager scriptManager, IScriptContextFactory contextFactory) {
        this.scriptManager = scriptManager;
        this.contextFactory = contextFactory;
    }

    /**
     * 统一脚本执行入口
     *
     * @param bizCode 业务脚本编码
     * @param params  合并后的请求参数（query params + body + headers）
     * @param track   是否开启追踪模式
     * @return 脚本执行结果
     */
    public ScriptResult execute(String bizCode, Map<String, Object> params, boolean track) {
        long startTime = System.currentTimeMillis();
        if (params == null) params = new HashMap<>();

        // 1. 获取脚本实例
        IBusinessScript script = scriptManager.getScript(bizCode);
        if (script == null) {
            throw new ScriptNotFoundException(bizCode);
        }

        // 2. 构建上下文（业务模块实现）
        Map<String, Object> scriptContext = contextFactory.buildContext(params);

        // 3. 注入 ScriptTracer（始终开启）
        ScriptTracer tracer = new ScriptTracer(true);
        scriptContext.put("t", tracer);

        // 4. track 模式：用代理包装 Bean 追踪调用
        List<BeanCallInfo> beanTrace = new ArrayList<>();
        if (track) {
            wrapBeansWithProxy(scriptContext, beanTrace);
        }

        try {
            // 5. 调度超时中断
            Thread executionThread = Thread.currentThread();
            ScheduledFuture<?> timeoutTask = null;
            int timeout = scriptManager.getTimeoutSeconds();
            if (timeout > 0) {
                timeoutTask = timeoutScheduler.schedule(
                        executionThread::interrupt, timeout, TimeUnit.SECONDS
                );
            }

            try {
                // 6. 执行脚本
                Object result = script.execute(scriptContext);
                long elapsed = System.currentTimeMillis() - startTime;

                SCRIPT_LOGGER.info("bizCode={} elapsed={}ms status=SUCCESS", bizCode, elapsed);
                if (elapsed > 3000) {
                    log.warn("脚本执行耗时过长: bizCode={}, elapsed={}ms", bizCode, elapsed);
                }

                // 7. 封装 ScriptResult
                List<String> traceLogs = tracer.getLogs().isEmpty() ? null : tracer.getLogs();

                Map<String, Object> trackInfo = null;
                if (track) {
                    trackInfo = new LinkedHashMap<>();
                    trackInfo.put("bizCode", bizCode);
                    trackInfo.put("version", scriptManager.getVersion(bizCode));
                    trackInfo.put("elapsed", elapsed);
                    trackInfo.put("beanCalls", beanTrace);
                }

                return ScriptResult.success(result, traceLogs, trackInfo, elapsed);
            } finally {
                if (timeoutTask != null) timeoutTask.cancel(false);
                Thread.interrupted();
            }
        } catch (GroovyEngineException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            SCRIPT_LOGGER.warn("bizCode={} elapsed={}ms status=BIZ_ERROR error={}", bizCode, elapsed, e.getMessage());
            throw e;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            if (isInterruptTimeout(e)) {
                int timeout = scriptManager.getTimeoutSeconds();
                SCRIPT_LOGGER.error("bizCode={} elapsed={}ms status=TIMEOUT", bizCode, elapsed);
                throw new ScriptTimeoutException(timeout);
            }
            String error = extractScriptError(e);
            SCRIPT_LOGGER.error("bizCode={} elapsed={}ms status=FAIL error={}", bizCode, elapsed, error);
            throw new ScriptExecutionException(error, e);
        }
    }

    /**
     * 获取代码补全数据（委托 contextFactory）
     */
    public Map<String, List<MethodInfo>> getCompletions() {
        return contextFactory.getCompletionData();
    }

    public ScriptManager getScriptManager() {
        return scriptManager;
    }

    /**
     * 用动态代理包装 context 中的 Bean（白名单策略）
     */
    private void wrapBeansWithProxy(Map<String, Object> context, List<BeanCallInfo> trace) {
        Set<String> proxyKeys = contextFactory.getCompletionData().keySet();
        for (String key : proxyKeys) {
            Object bean = context.get(key);
            if (bean == null) {
                continue;
            }
            Class<?>[] interfaces = bean.getClass().getInterfaces();
            if (interfaces.length > 0) {
                context.put(key, createTracingProxy(bean, key, trace));
            }
        }
    }

    private Object createTracingProxy(Object bean, String beanName, List<BeanCallInfo> trace) {
        return Proxy.newProxyInstance(
                bean.getClass().getClassLoader(),
                bean.getClass().getInterfaces(),
                (proxy, method, args) -> {
                    long start = System.currentTimeMillis();
                    Object result = method.invoke(bean, args);
                    trace.add(new BeanCallInfo(beanName, method.getName(),
                            args != null ? Arrays.toString(args) : "[]",
                            String.valueOf(result),
                            System.currentTimeMillis() - start));
                    return result;
                }
        );
    }

    private String extractScriptError(Exception e) {
        Throwable cause = e;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        StringBuilder msg = new StringBuilder(cause.getMessage() != null ? cause.getMessage() : "未知错误");
        for (StackTraceElement ste : e.getStackTrace()) {
            if (ste.getClassName().startsWith("Script") || ste.getClassName().contains("$")) {
                msg.append(" (line ").append(ste.getLineNumber()).append(")");
                break;
            }
        }
        return msg.toString();
    }

    private boolean isInterruptTimeout(Exception e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof InterruptedException) return true;
            cause = cause.getCause();
        }
        return false;
    }
}
