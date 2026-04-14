package cn.fengin.groovy.spring.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * Groovy Engine Web 配置
 * <p>
 * 注册鉴权拦截器和 CORS 配置，确保 Web IDE / Desktop IDE 能跨域访问。
 *
 * @author 凌封 (https://aibook.ren)
 */
@Configuration
public class GroovyWebConfig implements WebMvcConfigurer {

    @Resource
    private GroovyScriptAuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/groovy/script/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/groovy/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
