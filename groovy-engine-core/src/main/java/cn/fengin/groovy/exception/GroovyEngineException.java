package cn.fengin.groovy.exception;

/**
 * Groovy Engine 基础异常
 * <p>
 * 所有引擎相关异常的基类。业务方在脚本中也可以直接
 * {@code throw new GroovyEngineException("业务错误")} 来抛出可控业务异常。
 *
 * @author 凌封 (https://aibook.ren)
 */
public class GroovyEngineException extends RuntimeException {

    public GroovyEngineException(String message) {
        super(message);
    }

    public GroovyEngineException(String message, Throwable cause) {
        super(message, cause);
    }
}
