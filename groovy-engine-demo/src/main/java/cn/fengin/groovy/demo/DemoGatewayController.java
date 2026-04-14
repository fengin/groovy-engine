package cn.fengin.groovy.demo;

import cn.fengin.groovy.spring.web.AbstractGroovyGatewayController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * Demo 业务网关
 * <p>
 * 演示如何继承 {@link AbstractGroovyGatewayController} 暴露业务端点。
 * 生产环境可添加权限注解（如 {@code @PreAuthorize}）。
 *
 * @author 凌封 (https://aibook.ren)
 */
@RestController
@RequestMapping("/api/demo/gw")
public class DemoGatewayController extends AbstractGroovyGatewayController {

    @RequestMapping(value = "/{bizCode}", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> executeBiz(
            @PathVariable String bizCode,
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest request) {
        return execute(bizCode, body, request);
    }
}
