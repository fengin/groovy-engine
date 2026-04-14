package cn.fengin.groovy.engine;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 脚本追踪工具 — 注入到 ctx.t
 * <p>
 * 收集脚本中 t.log() 的输出，最终拼到 _trace 返回。
 * 返回 List&lt;String&gt;，每条格式："HH:mm:ss:SS, 日志内容"，按时间顺序排列。
 * <p>
 * 使用方式（Groovy 脚本中）：
 * <pre>
 *   def t = ctx.t
 *   t.log("开始计算")                              // 纯文本
 *   t.log("configId={}", configId)                 // 单占位符
 *   t.log("用户ID：{},用户名：{}", 31313, "测试用户")  // 多占位符
 * </pre>
 *
 * @author 凌封 (https://aibook.ren)
 */
public class ScriptTracer {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss:SS");

    private final List<String> logs = new ArrayList<>();
    private final boolean enabled;

    public ScriptTracer(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 记录日志（支持占位符）
     *
     * @param pattern 日志模板，{} 为占位符
     * @param args    占位符对应的值（可变参数）
     */
    public void log(String pattern, Object... args) {
        if (!enabled || pattern == null) return;

        String message;
        if (args == null || args.length == 0) {
            message = pattern;
        } else {
            message = formatMessage(pattern, args);
        }

        String timestamp = LocalTime.now().format(TIME_FMT);
        logs.add(timestamp + ", " + message);
    }

    /**
     * 获取所有追踪日志（按记录时间顺序）
     */
    public List<String> getLogs() {
        return logs;
    }

    /**
     * 简易占位符替换：将 pattern 中的 {} 依次替换为 args 的值
     */
    private static String formatMessage(String pattern, Object[] args) {
        StringBuilder sb = new StringBuilder(pattern.length() + 32);
        int argIndex = 0;
        int i = 0;
        while (i < pattern.length()) {
            if (i + 1 < pattern.length() && pattern.charAt(i) == '{' && pattern.charAt(i + 1) == '}') {
                if (argIndex < args.length) {
                    sb.append(args[argIndex++]);
                } else {
                    sb.append("{}");
                }
                i += 2;
            } else {
                sb.append(pattern.charAt(i));
                i++;
            }
        }
        return sb.toString();
    }
}
