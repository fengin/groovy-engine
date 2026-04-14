package cn.fengin.groovy.spring.web;

import cn.fengin.groovy.engine.ScriptExecutionService;
import cn.fengin.groovy.exception.GroovyEngineException;
import cn.fengin.groovy.model.ScriptResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.net.URLEncoder;
import java.util.*;

/**
 * Groovy 脚本业务网关抽象基类
 * <p>
 * 提供统一的 HTTP 请求参数解析和脚本执行入口，各业务模块继承此类
 * 并添加自己的 URL 前缀和权限注解即可。
 *
 * <h3>子类实现示例</h3>
 * <pre>{@code
 * @RestController
 * @RequestMapping("/api/my/gw")
 * public class MyGatewayController extends AbstractGroovyGatewayController {
 *
 *     @RequestMapping(value = "/{bizCode}", method = {GET, POST})
 *     public ResponseEntity<?> executeBiz(
 *             @PathVariable String bizCode,
 *             @RequestBody(required = false) Map<String, Object> body,
 *             HttpServletRequest request) {
 *         return execute(bizCode, body, request);
 *     }
 * }
 * }</pre>
 *
 * @author 凌封 (https://aibook.ren)
 */
public abstract class AbstractGroovyGatewayController {

    private static final Logger log = LoggerFactory.getLogger(AbstractGroovyGatewayController.class);

    @Resource
    private ScriptExecutionService scriptExecutionService;

    /**
     * 统一业务网关执行入口 — 同时支持 GET 和 POST
     */
    protected ResponseEntity<?> execute(String bizCode, Map<String, Object> body,
                                        HttpServletRequest request) {
        try {
            Map<String, Object> params = buildParams(body, request);
            ScriptResult result = scriptExecutionService.execute(bizCode, params, false);
            Object data = result.getData();

            // 脚本返回 byte[] → 自动切换为文件下载
            if (data instanceof byte[]) {
                String filename = params.containsKey("_filename")
                        ? String.valueOf(params.get("_filename")) : "report.xlsx";
                try {
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .header("Content-Disposition",
                                    "attachment; filename=" + URLEncoder.encode(filename, "UTF-8"))
                            .body((byte[]) data);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            return ResponseEntity.ok(result);
        } catch (GroovyEngineException e) {
            log.warn("脚本业务异常: bizCode={}, error={}", bizCode, e.getMessage());
            return ResponseEntity.ok(ScriptResult.error(e.getMessage()));
        } catch (Exception e) {
            log.error("脚本执行异常: bizCode={}", bizCode, e);
            return ResponseEntity.ok(ScriptResult.error("系统异常: " + e.getMessage()));
        }
    }

    /**
     * 统一参数构建：合并 Query Params + Body + Headers
     */
    private Map<String, Object> buildParams(Map<String, Object> body, HttpServletRequest request) {
        Map<String, Object> params = new HashMap<>();

        // 1. Query Params
        Map<String, String[]> queryParams = request.getParameterMap();
        if (queryParams != null) {
            for (Map.Entry<String, String[]> entry : queryParams.entrySet()) {
                String[] values = entry.getValue();
                params.put(entry.getKey(), values.length == 1 ? values[0] : Arrays.asList(values));
            }
        }

        // 2. Body 覆盖 Query Params
        if (body != null) {
            params.putAll(body);
        }

        // 3. 注入 Headers
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                headers.put(name, request.getHeader(name));
            }
        }
        params.put("_headers", headers);
        params.put("_method", request.getMethod());
        params.put("_uri", request.getRequestURI());

        return params;
    }
}
