package cn.fengin.groovy.exception;

/**
 * 脚本编译异常 — Groovy 语法错误或类型错误
 *
 * @author 凌封 (https://aibook.ren)
 */
public class ScriptCompilationException extends GroovyEngineException {

    public ScriptCompilationException(String bizCode, Throwable cause) {
        super("脚本编译失败[" + bizCode + "]: " + cause.getMessage(), cause);
    }
}
