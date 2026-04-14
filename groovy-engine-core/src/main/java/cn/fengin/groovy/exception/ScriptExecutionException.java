package cn.fengin.groovy.exception;

/**
 * 脚本运行时执行异常（非业务异常）
 *
 * @author 凌封 (https://aibook.ren)
 */
public class ScriptExecutionException extends GroovyEngineException {

    public ScriptExecutionException(String message) {
        super("业务执行异常: " + message);
    }

    public ScriptExecutionException(String message, Throwable cause) {
        super("业务执行异常: " + message, cause);
    }
}
