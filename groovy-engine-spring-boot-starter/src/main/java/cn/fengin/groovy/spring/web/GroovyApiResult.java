package cn.fengin.groovy.spring.web;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Groovy Engine 管理接口通用响应包装
 * <p>
 * 用于脚本 CRUD、刷新等管理端接口返回，结构与常见 REST API 保持一致：
 * {@code {code, message, data}}。
 * <p>
 * 与 {@link cn.fengin.groovy.model.ScriptResult} 不同，本类是通用的管理接口响应，
 * 不含执行追踪（_trace/_track）等调试字段。
 *
 * @author 凌封 (https://aibook.ren)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GroovyApiResult<T> {

    private int code;
    private String message;
    private T data;

    public static <T> GroovyApiResult<T> success(T data) {
        GroovyApiResult<T> r = new GroovyApiResult<>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static <T> GroovyApiResult<T> failed(String message) {
        GroovyApiResult<T> r = new GroovyApiResult<>();
        r.code = 500;
        r.message = message;
        return r;
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
