package cn.fengin.groovy.model;

import java.util.List;
import java.util.Map;

/**
 * Groovy 脚本统一响应结构
 * <p>
 * 基于接口规范 {@code {code, message, data}} 扩展调试字段。
 * 所有 GroovyEngine 相关接口统一返回此类型。
 * <p>
 * 扩展字段（仅调试/开发场景有值，生产环境自动隐藏 _track）：
 * <ul>
 *   <li>{@code _trace} — t.log() 收集的追踪日志</li>
 *   <li>{@code _track} — Bean 调用链追踪（track 模式）</li>
 *   <li>{@code _error} — 异常标记（脚本执行异常时为 true）</li>
 *   <li>{@code cost} — 脚本执行耗时（毫秒）</li>
 * </ul>
 * <p>
 * 注意：JSON 序列化行为（如 null 字段隐藏）由 Spring Boot Starter 层的
 * Jackson ObjectMapper 配置控制，core 模块不依赖 Jackson。
 *
 * @author 凌封 (https://aibook.ren)
 */
public class ScriptResult {

    /** 业务状态码 */
    private Integer code;

    /** 响应消息 */
    private String message;

    /** 脚本返回的业务数据 */
    private Object data;

    /** t.log() 追踪日志（有内容才输出） */
    private List<String> _trace;

    /** track 模式 Bean 调用链（track=true 时才有） */
    private Map<String, Object> _track;

    /** 异常标记（脚本执行异常时为 true） */
    private Boolean _error;

    /** 异常堆栈详情（仅 test 接口异常时返回） */
    private String _stackTrace;

    /** 脚本执行耗时（毫秒） */
    private Long cost;

    // ==================== 静态工厂方法 ====================

    /**
     * 成功响应
     */
    public static ScriptResult success(Object data, List<String> trace,
                                       Map<String, Object> track, long cost) {
        ScriptResult r = new ScriptResult();
        r.setCode(200);
        r.setMessage("success");
        r.setData(data);
        r.set_trace(trace != null && !trace.isEmpty() ? trace : null);
        r.set_track(track);
        r.setCost(cost);
        return r;
    }

    /**
     * 业务异常响应
     */
    public static ScriptResult error(String message) {
        ScriptResult r = new ScriptResult();
        r.setCode(500);
        r.setMessage(message);
        return r;
    }

    /**
     * 脚本执行异常响应（携带堆栈，用于 Web IDE 调试）
     */
    public static ScriptResult errorWithStack(String message, String stackTrace) {
        ScriptResult r = new ScriptResult();
        r.setCode(500);
        r.setMessage(message);
        r.set_error(true);
        r.set_stackTrace(stackTrace);
        return r;
    }

    // ==================== Getter / Setter ====================

    public Integer getCode() { return code; }
    public void setCode(Integer code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }
    public List<String> get_trace() { return _trace; }
    public void set_trace(List<String> _trace) { this._trace = _trace; }
    public Map<String, Object> get_track() { return _track; }
    public void set_track(Map<String, Object> _track) { this._track = _track; }
    public Boolean get_error() { return _error; }
    public void set_error(Boolean _error) { this._error = _error; }
    public String get_stackTrace() { return _stackTrace; }
    public void set_stackTrace(String _stackTrace) { this._stackTrace = _stackTrace; }
    public Long getCost() { return cost; }
    public void setCost(Long cost) { this.cost = cost; }
}
