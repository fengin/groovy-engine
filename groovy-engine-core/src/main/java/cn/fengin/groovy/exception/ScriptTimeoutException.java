package cn.fengin.groovy.exception;

/**
 * 脚本执行超时异常
 *
 * @author 凌封 (https://aibook.ren)
 */
public class ScriptTimeoutException extends GroovyEngineException {

    public ScriptTimeoutException(int timeoutSeconds) {
        super("脚本执行超时（" + timeoutSeconds + "秒）");
    }
}
