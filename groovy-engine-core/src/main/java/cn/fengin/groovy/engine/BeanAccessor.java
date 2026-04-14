package cn.fengin.groovy.engine;

import java.util.Set;

/**
 * Bean 安全访问器 — 限制脚本只能通过白名单包获取 Bean。
 * <p>
 * 禁止获取 DataSource、TransactionManager 等基础设施 Bean。
 * <p>
 * 使用方式（在 IScriptContextFactory 中）：
 * <pre>{@code
 * Set<String> allowed = new HashSet<>(Arrays.asList("com.myapp.service", "com.myapp.domain"));
 * ctx.put("getBean", new BeanAccessor(applicationContext, allowed));
 * }</pre>
 * 脚本中使用：
 * <pre>{@code
 * def myService = ctx.getBean.get("myServiceImpl")
 * }</pre>
 *
 * @author 凌封 (https://aibook.ren)
 */
public class BeanAccessor {

    private final Object applicationContext;
    private final Set<String> allowedPackagePrefixes;

    /**
     * @param applicationContext    Spring ApplicationContext（Object 类型避免 core 依赖 Spring）
     * @param allowedPackagePrefixes 允许脚本访问的包前缀白名单
     */
    public BeanAccessor(Object applicationContext, Set<String> allowedPackagePrefixes) {
        this.applicationContext = applicationContext;
        this.allowedPackagePrefixes = allowedPackagePrefixes;
    }

    /**
     * 按 Spring Bean 名称获取
     *
     * @param beanName Spring 容器中的 Bean 名称
     */
    public Object get(String beanName) {
        try {
            java.lang.reflect.Method getBean = applicationContext.getClass().getMethod("getBean", String.class);
            Object bean = getBean.invoke(applicationContext, beanName);
            checkAccess(bean, beanName);
            return bean;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("获取 Bean 失败: " + beanName, e);
        }
    }

    /**
     * 按类型获取
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> clazz) {
        try {
            java.lang.reflect.Method getBean = applicationContext.getClass().getMethod("getBean", Class.class);
            T bean = (T) getBean.invoke(applicationContext, clazz);
            checkAccess(bean, clazz.getName());
            return bean;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("获取 Bean 失败: " + clazz.getName(), e);
        }
    }

    private void checkAccess(Object bean, String identifier) {
        String pkg = bean.getClass().getPackage().getName();
        boolean allowed = false;
        for (String prefix : allowedPackagePrefixes) {
            if (pkg.startsWith(prefix)) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            throw new RuntimeException("脚本禁止访问该 Bean: " + identifier
                    + " (package: " + pkg + ")");
        }
    }
}
