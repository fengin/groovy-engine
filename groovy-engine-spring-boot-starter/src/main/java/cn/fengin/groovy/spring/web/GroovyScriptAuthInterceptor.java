package cn.fengin.groovy.spring.web;

import cn.fengin.groovy.spring.autoconfigure.GroovyEngineProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Groovy 脚本管理接口鉴权拦截器
 * <p>
 * 拦截 /api/groovy/script/** 路径，通过 X-Groovy-Token Header 进行 API Key 鉴权。
 *
 * @author 凌封 (https://aibook.ren)
 */
@Component
public class GroovyScriptAuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(GroovyScriptAuthInterceptor.class);

    @Resource
    private GroovyEngineProperties properties;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        GroovyEngineProperties.Manage manage = properties.getManage();

        // 管理接口未启用
        if (!manage.isEnabled() || !StringUtils.hasText(manage.getApiKey())) {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"message\":\"脚本管理接口未启用\"}");
            return false;
        }

        // 校验 Token
        String token = request.getHeader("X-Groovy-Token");
        if (!manage.getApiKey().equals(token)) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"鉴权失败\"}");
            return false;
        }

        return true;
    }
}
