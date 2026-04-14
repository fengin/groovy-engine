package cn.fengin.groovy.exception;

/**
 * 脚本未找到异常 — bizCode 不存在或已禁用
 *
 * @author 凌封 (https://aibook.ren)
 */
public class ScriptNotFoundException extends GroovyEngineException {

    public ScriptNotFoundException(String bizCode) {
        super("未找到业务脚本: " + bizCode);
    }
}
