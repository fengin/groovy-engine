package cn.fengin.groovy.engine;

import cn.fengin.groovy.api.IBusinessScript;
import cn.fengin.groovy.api.IScriptStorage;
import cn.fengin.groovy.exception.ScriptCompilationException;
import cn.fengin.groovy.model.GroovyScript;
import groovy.lang.GroovyClassLoader;
import groovy.transform.ThreadInterrupt;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Groovy 脚本编译 + 缓存管理器（核心组件）
 * <p>
 * 纯 Java 实现，无 Spring 依赖。由 Starter 的 AutoConfiguration 负责实例化和注入。
 * <p>
 * 生命周期：
 * 1. 构造时预加载所有已启用脚本（preloadAllScripts）
 * 2. 正常请求从 cache 取，无编译
 * 3. refreshScript 单个刷新 + 保底 ClassLoader 回收
 * 4. reloadAll 全量重载 + 必定回收 ClassLoader
 *
 * @author 凌封 (https://aibook.ren)
 */
public class ScriptManager {

    private static final Logger log = LoggerFactory.getLogger(ScriptManager.class);

    private final ConcurrentHashMap<String, IBusinessScript> scriptCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Integer> versionCache = new ConcurrentHashMap<>();

    private volatile GroovyClassLoader currentClassLoader;
    private final AtomicInteger compileCounter = new AtomicInteger(0);

    private final IScriptStorage scriptStorage;

    /** ClassLoader 回收阈值 */
    private final int recycleThreshold;

    /** 脚本执行超时（秒），防止死循环。默认 600s（10分钟）兜底，0 表示不限制 */
    private final int timeoutSeconds;

    /**
     * @param scriptStorage    脚本存储实现
     * @param recycleThreshold ClassLoader 回收阈值（编译累计次数），建议 100
     * @param timeoutSeconds   脚本执行超时秒数，0 表示不限制
     */
    public ScriptManager(IScriptStorage scriptStorage, int recycleThreshold, int timeoutSeconds) {
        this.scriptStorage = scriptStorage;
        this.recycleThreshold = recycleThreshold;
        this.timeoutSeconds = timeoutSeconds;
        this.currentClassLoader = createClassLoader();
        preloadAllScripts();
    }

    /**
     * 获取已编译的脚本实例（优先缓存）
     */
    public IBusinessScript getScript(String bizCode) {
        IBusinessScript cached = scriptCache.get(bizCode);
        if (cached != null) {
            return cached;
        }
        return loadAndCompileScript(bizCode);
    }

    /**
     * 获取脚本版本号
     */
    public Integer getVersion(String bizCode) {
        return versionCache.get(bizCode);
    }

    /**
     * 刷新指定脚本（管理端/Web IDE 调用）
     */
    public void refreshScript(String bizCode) {
        scriptCache.remove(bizCode);
        versionCache.remove(bizCode);
        loadAndCompileScript(bizCode);
        log.info("脚本已刷新: bizCode={}", bizCode);
    }

    /**
     * 重新加载全部 — 必定回收 ClassLoader
     */
    public void reloadAll() {
        GroovyClassLoader old = this.currentClassLoader;
        this.currentClassLoader = createClassLoader();
        scriptCache.clear();
        versionCache.clear();
        compileCounter.set(0);
        preloadAllScripts();
        closeClassLoaderQuietly(old);
        log.info("全部脚本已重载, 缓存: {}", scriptCache.size());
    }

    public int getCacheSize() {
        return scriptCache.size();
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    // ==================== 内部方法 ====================

    private synchronized IBusinessScript loadAndCompileScript(String bizCode) {
        // 双重检查
        IBusinessScript cached = scriptCache.get(bizCode);
        if (cached != null) {
            return cached;
        }

        GroovyScript entity = scriptStorage.getByBizCode(bizCode);
        if (entity == null) {
            log.warn("脚本不存在或已禁用: bizCode={}", bizCode);
            return null;
        }

        try {
            // 保底回收策略：累计编译达到阈值时回收 ClassLoader
            if (compileCounter.incrementAndGet() >= recycleThreshold) {
                log.info("编译次数达到阈值 {}，回收 ClassLoader", recycleThreshold);
                GroovyClassLoader old = this.currentClassLoader;
                this.currentClassLoader = createClassLoader();
                scriptCache.clear();
                versionCache.clear();
                compileCounter.set(1); // 当前这次编译算第 1 次
                closeClassLoaderQuietly(old);
                preloadAllScripts();
                // 重新编译后缓存中应该已有该脚本
                return scriptCache.get(bizCode);
            }

            Class<?> clazz = currentClassLoader.parseClass(entity.getScriptContent());
            Object instance = clazz.getDeclaredConstructor().newInstance();

            if (!(instance instanceof IBusinessScript)) {
                throw new RuntimeException("脚本必须实现 IBusinessScript 接口: " + bizCode);
            }

            IBusinessScript script = (IBusinessScript) instance;
            scriptCache.put(bizCode, script);
            versionCache.put(bizCode, entity.getVersion());
            log.info("脚本编译成功: bizCode={}, version={}", bizCode, entity.getVersion());
            return script;

        } catch (Exception e) {
            log.error("脚本编译失败: bizCode={}", bizCode, e);
            throw new ScriptCompilationException(bizCode, e);
        }
    }

    private GroovyClassLoader createClassLoader() {
        CompilerConfiguration config = new CompilerConfiguration();
        // 脚本中断保护：编译时在循环/方法入口注入 Thread.isInterrupted() 检查
        if (timeoutSeconds > 0) {
            config.addCompilationCustomizers(
                    new ASTTransformationCustomizer(ThreadInterrupt.class)
            );
            log.info("Groovy 脚本线程中断保护已启用, 超时: {}s", timeoutSeconds);
        }
        return new GroovyClassLoader(this.getClass().getClassLoader(), config);
    }

    private void preloadAllScripts() {
        try {
            List<GroovyScript> scripts = scriptStorage.listEnabledScripts();
            if (scripts != null) {
                for (GroovyScript s : scripts) {
                    try {
                        // 直接编译，不走 compileCounter（预加载不算增量编译）
                        Class<?> clazz = currentClassLoader.parseClass(s.getScriptContent());
                        Object instance = clazz.getDeclaredConstructor().newInstance();
                        if (instance instanceof IBusinessScript) {
                            scriptCache.put(s.getBizCode(), (IBusinessScript) instance);
                            versionCache.put(s.getBizCode(), s.getVersion());
                        }
                    } catch (Exception e) {
                        log.error("预加载失败: bizCode={}", s.getBizCode(), e);
                    }
                }
                log.info("脚本预加载完成, 共 {} 个", scriptCache.size());
            }
        } catch (Exception e) {
            log.error("脚本预加载异常", e);
        }
    }

    private void closeClassLoaderQuietly(GroovyClassLoader cl) {
        try {
            if (cl != null) {
                cl.close();
            }
        } catch (Exception e) {
            log.warn("关闭旧 ClassLoader 异常", e);
        }
    }
}
