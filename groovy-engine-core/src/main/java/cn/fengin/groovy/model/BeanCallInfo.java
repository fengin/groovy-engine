package cn.fengin.groovy.model;

/**
 * Bean 调用追踪信息（track 模式用）
 *
 * @author 凌封 (https://aibook.ren)
 */
public class BeanCallInfo {

    /** Bean 名称 */
    private String bean;
    /** 方法名 */
    private String method;
    /** 调用参数 */
    private String args;
    /** 返回值 */
    private String result;
    /** 耗时(ms) */
    private long elapsed;

    public BeanCallInfo(String bean, String method, String args, String result, long elapsed) {
        this.bean = bean;
        this.method = method;
        this.args = args;
        this.result = result;
        this.elapsed = elapsed;
    }

    public String getBean() { return bean; }
    public void setBean(String bean) { this.bean = bean; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getArgs() { return args; }
    public void setArgs(String args) { this.args = args; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public long getElapsed() { return elapsed; }
    public void setElapsed(long elapsed) { this.elapsed = elapsed; }
}
