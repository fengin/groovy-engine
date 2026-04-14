package cn.fengin.groovy.spring.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Groovy Engine 配置属性
 *
 * @author 凌封 (https://aibook.ren)
 */
@ConfigurationProperties(prefix = "groovy.engine")
public class GroovyEngineProperties {

    /** 总开关（默认 true） */
    private boolean enabled = true;

    /** 管理接口配置 */
    private Manage manage = new Manage();

    /** 脚本执行配置 */
    private Script script = new Script();

    /** ClassLoader 配置 */
    private Classloader classloader = new Classloader();

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Manage getManage() { return manage; }
    public void setManage(Manage manage) { this.manage = manage; }
    public Script getScript() { return script; }
    public void setScript(Script script) { this.script = script; }
    public Classloader getClassloader() { return classloader; }
    public void setClassloader(Classloader classloader) { this.classloader = classloader; }

    public static class Manage {
        /** IDE 管理接口鉴权 key */
        private String apiKey = "";
        /** 管理接口开关（默认 true） */
        private boolean enabled = true;

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class Script {
        /** 脚本执行超时（秒），0 表示不限制 */
        private int timeoutSeconds = 600;

        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    }

    public static class Classloader {
        /** ClassLoader 回收阈值 */
        private int recycleThreshold = 100;

        public int getRecycleThreshold() { return recycleThreshold; }
        public void setRecycleThreshold(int recycleThreshold) { this.recycleThreshold = recycleThreshold; }
    }
}
