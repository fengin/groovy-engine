package cn.fengin.groovy.spring.web;

import cn.fengin.groovy.api.IScriptStorage;
import cn.fengin.groovy.engine.ScriptExecutionService;
import cn.fengin.groovy.engine.ScriptManager;
import cn.fengin.groovy.model.GroovyScript;
import cn.fengin.groovy.model.MethodInfo;
import cn.fengin.groovy.model.ScriptResult;
import cn.fengin.groovy.spring.web.dto.ScriptDeployRequest;
import cn.fengin.groovy.spring.web.dto.ScriptTestRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;

/**
 * Groovy 脚本管理 Controller
 * <p>
 * 提供脚本 CRUD + 测试执行 + 批量部署 + 代码补全。
 * 所有接口均需 X-Groovy-Token Header（由 GroovyScriptAuthInterceptor 拦截）。
 *
 * @author 凌封 (https://aibook.ren)
 */
@RestController
@RequestMapping("/api/groovy/script")
public class GroovyScriptManageController {

    @Resource
    private IScriptStorage scriptStorage;

    @Resource
    private ScriptManager scriptManager;

    @Resource
    private ScriptExecutionService scriptExecutionService;

    /**
     * ① 脚本列表（不返回 scriptContent）
     */
    @GetMapping("/list")
    public GroovyApiResult<List<GroovyScript>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String projectCode) {
        return GroovyApiResult.success(scriptStorage.listScripts(category, projectCode));
    }

    /**
     * ② 脚本详情（含 scriptContent）
     */
    @GetMapping("/{id}")
    public GroovyApiResult<GroovyScript> getById(@PathVariable Long id) {
        return GroovyApiResult.success(scriptStorage.getById(id));
    }

    /**
     * ③ 新建脚本
     */
    @PostMapping
    public GroovyApiResult<Void> create(@RequestBody GroovyScript script) {
        if (script == null || !StringUtils.hasText(script.getBizCode()) || !StringUtils.hasText(script.getScriptContent())) {
            return GroovyApiResult.failed("bizCode 和 scriptContent 不能为空");
        }
        if (scriptStorage.getByBizCode(script.getBizCode()) != null) {
            return GroovyApiResult.failed("bizCode 已存在: " + script.getBizCode());
        }
        script.setVersion(1);
        script.setStatus(1);
        scriptStorage.save(script);
        return GroovyApiResult.success(null);
    }

    /**
     * ④ 更新脚本（保存后自动刷新缓存）
     */
    @PutMapping("/{id}")
    public GroovyApiResult<Integer> update(@PathVariable Long id, @RequestBody GroovyScript script) {
        GroovyScript existing = scriptStorage.getById(id);
        if (existing == null) {
            return GroovyApiResult.failed("脚本不存在: id=" + id);
        }
        // 乐观锁
        if (script.getVersion() != null && !script.getVersion().equals(existing.getVersion())) {
            return GroovyApiResult.failed("版本冲突: 当前服务器版本 v" + existing.getVersion()
                    + "，你基于 v" + script.getVersion() + " 编辑。请刷新后重试");
        }
        existing.setVersion(existing.getVersion() + 1);
        if (StringUtils.hasText(script.getScriptContent())) {
            existing.setScriptContent(script.getScriptContent());
        }
        if (StringUtils.hasText(script.getName())) {
            existing.setName(script.getName());
        }
        if (script.getCategory() != null) {
            existing.setCategory(script.getCategory());
        }
        if (script.getProjectCode() != null) {
            existing.setProjectCode(script.getProjectCode());
        }
        if (script.getRemark() != null) {
            existing.setRemark(script.getRemark());
        }
        if (script.getStatus() != null) {
            existing.setStatus(script.getStatus());
        }
        scriptStorage.updateById(existing);
        
        try {
            scriptManager.refreshScript(existing.getBizCode());
        } catch (Exception e) {
            GroovyApiResult<Integer> result = GroovyApiResult.success(existing.getVersion());
            result.setMessage("保存成功，但脚本存在语法错误（无法加载更新）: " + e.getMessage());
            return result;
        }
        
        GroovyApiResult<Integer> result = GroovyApiResult.success(existing.getVersion());
        result.setMessage("保存成功");
        return result;
    }

    /**
     * ⑤ 删除脚本
     */
    @DeleteMapping("/{id}")
    public GroovyApiResult<Void> delete(@PathVariable Long id) {
        GroovyScript existing = scriptStorage.getById(id);
        scriptStorage.removeById(id);
        if (existing != null) {
            scriptManager.refreshScript(existing.getBizCode());
        }
        return GroovyApiResult.success(null);
    }

    /**
     * ⑥ 测试执行（支持 trace + track 模式）
     */
    @PostMapping("/test")
    public ResponseEntity<?> test(@RequestBody ScriptTestRequest req) {
        if (req == null || !StringUtils.hasText(req.getBizCode())) {
            return ResponseEntity.ok(ScriptResult.error("bizCode 不能为空"));
        }
        try {
            ScriptResult result = scriptExecutionService.execute(
                    req.getBizCode(), req.getParams(), Boolean.TRUE.equals(req.getTrack()));

            if (result.getData() instanceof byte[]) {
                String filename = (req.getParams() != null && req.getParams().containsKey("_filename"))
                        ? String.valueOf(req.getParams().get("_filename")) : "test_download.xlsx";
                try {
                    return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .header("Content-Disposition",
                                    "attachment; filename=" + URLEncoder.encode(filename, "UTF-8"))
                            .body((byte[]) result.getData());
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            StringBuilder stack = new StringBuilder();
            Throwable cause = e;
            while (cause != null) {
                stack.append(cause.getClass().getSimpleName()).append(": ").append(cause.getMessage()).append("\n");
                for (StackTraceElement ste : cause.getStackTrace()) {
                    String cls = ste.getClassName();
                    if (cls.startsWith("Script") || cls.contains("$")
                            || cls.startsWith("cn.fengin")) {
                        stack.append("  at ").append(cls).append(".").append(ste.getMethodName())
                                .append("(").append(ste.getFileName()).append(":").append(ste.getLineNumber()).append(")\n");
                    }
                }
                cause = cause.getCause();
                if (cause != null) {
                    stack.append("Caused by: ");
                }
            }
            return ResponseEntity.ok(ScriptResult.errorWithStack(e.getMessage(), stack.toString()));
        }
    }

    /**
     * ⑦ 批量部署
     */
    @PostMapping("/deploy")
    public GroovyApiResult<String> deploy(@RequestBody List<ScriptDeployRequest> scripts) {
        if (scripts == null || scripts.isEmpty()) {
            return GroovyApiResult.success("新增:0 更新:0");
        }
        int created = 0, updated = 0;
        for (ScriptDeployRequest req : scripts) {
            if (req == null || !StringUtils.hasText(req.getBizCode())) {
                continue;
            }
            GroovyScript existing = scriptStorage.getByBizCode(req.getBizCode());
            if (existing != null) {
                existing.setScriptContent(req.getScriptContent());
                existing.setName(req.getName());
                existing.setCategory(req.getCategory());
                existing.setRemark(req.getRemark());
                existing.setVersion(existing.getVersion() + 1);
                scriptStorage.updateById(existing);
                updated++;
            } else {
                GroovyScript entity = new GroovyScript();
                BeanUtils.copyProperties(req, entity);
                entity.setVersion(1);
                entity.setStatus(1);
                scriptStorage.save(entity);
                created++;
            }
        }
        scriptManager.reloadAll();
        return GroovyApiResult.success("新增:" + created + " 更新:" + updated);
    }

    /**
     * ⑧ 代码补全数据
     */
    @GetMapping("/completions")
    public GroovyApiResult<Map<String, List<MethodInfo>>> getCompletions() {
        return GroovyApiResult.success(scriptExecutionService.getCompletions());
    }

    /**
     * ⑨ 刷新指定脚本缓存
     */
    @PostMapping("/refresh/{bizCode}")
    public GroovyApiResult<String> refresh(@PathVariable String bizCode) {
        try {
            scriptManager.refreshScript(bizCode);
            return GroovyApiResult.success("已刷新: " + bizCode);
        } catch (Exception e) {
            return GroovyApiResult.failed("刷新失败[" + bizCode + "]: " + e.getMessage());
        }
    }

    /**
     * ⑩ 刷新全部
     */
    @PostMapping("/refresh/all")
    public GroovyApiResult<String> refreshAll() {
        scriptManager.reloadAll();
        return GroovyApiResult.success("已刷新全部, 缓存: " + scriptManager.getCacheSize());
    }

    /**
     * ⑪ 保存或更新接口文档
     */
    @PostMapping("/doc")
    public GroovyApiResult<Void> saveDoc(@RequestBody Map<String, String> req) {
        String bizCode = req.get("bizCode");
        String docContent = req.get("docContent");
        if (!StringUtils.hasText(bizCode)) {
            return GroovyApiResult.failed("bizCode 不能为空");
        }
        GroovyScript existing = scriptStorage.getByBizCode(bizCode);
        if (existing == null) {
            return GroovyApiResult.failed("脚本不存在: " + bizCode);
        }
        existing.setDocContent(docContent);
        scriptStorage.updateById(existing);
        return GroovyApiResult.success(null);
    }

    /**
     * ⑫ 获取接口文档 JSON
     */
    @GetMapping("/doc/{bizCode}")
    public GroovyApiResult<String> getDoc(@PathVariable String bizCode) {
        GroovyScript existing = scriptStorage.getByBizCode(bizCode);
        if (existing == null) {
            return GroovyApiResult.failed("脚本不存在: " + bizCode);
        }
        return GroovyApiResult.success(existing.getDocContent());
    }

    /**
     * ⑬ 免密独立网页分享接口文档
     */
    @GetMapping(value = "/doc/share/{bizCode}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> shareDoc(@PathVariable String bizCode) {
        GroovyScript existing = scriptStorage.getByBizCode(bizCode);
        if (existing == null) {
            return ResponseEntity.status(404).body("<h3>脚本不存在: " + bizCode + "</h3>");
        }
        String html = renderDocHtml(existing.getBizCode(), existing.getName(), existing.getDocContent());
        return ResponseEntity.ok(html);
    }

    private String renderDocHtml(String bizCode, String name, String docContent) {
        String markdown = "";
        String inputExample = "";
        String outputExample = "";
        String requestUri = "/api/gateway/groovy/execute";
        
        StringBuilder headersTable = new StringBuilder("| Header名称 | 是否必填 | 说明 |\n|---|---|---|\n");
        StringBuilder inputsTable = new StringBuilder("| 参数名 | 类型 | 是否必填 | 说明 |\n|---|---|---|---|\n");
        
        boolean hasHeaders = false;
        boolean hasInputs = false;

        if (docContent != null && !docContent.trim().isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> map = mapper.readValue(docContent, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>(){});
                
                if (map.containsKey("requestUri") && map.get("requestUri") != null) {
                    requestUri = String.valueOf(map.get("requestUri"));
                }
                if (map.containsKey("inputExample") && map.get("inputExample") != null) {
                    inputExample = String.valueOf(map.get("inputExample"));
                }
                if (map.containsKey("outputExample") && map.get("outputExample") != null) {
                    outputExample = String.valueOf(map.get("outputExample"));
                }
                
                if (map.get("headers") instanceof List) {
                    List<?> list = (List<?>) map.get("headers");
                    for (Object item : list) {
                        if (item instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> m = (Map<String, Object>) item;
                            String hName = String.valueOf(m.getOrDefault("name", ""));
                            String req = Boolean.TRUE.equals(m.get("required")) ? "是" : "否";
                            String remark = String.valueOf(m.getOrDefault("remark", ""));
                            headersTable.append("| ").append(hName).append(" | ").append(req).append(" | ").append(remark).append(" |\n");
                            hasHeaders = true;
                        }
                    }
                }
                
                if (map.get("inputs") instanceof List) {
                    List<?> list = (List<?>) map.get("inputs");
                    for (Object item : list) {
                        if (item instanceof Map) {
                            @SuppressWarnings("unchecked")
                            Map<String, Object> m = (Map<String, Object>) item;
                            String fName = String.valueOf(m.getOrDefault("field", ""));
                            String type = String.valueOf(m.getOrDefault("type", ""));
                            String req = Boolean.TRUE.equals(m.get("required")) ? "是" : "否";
                            String remark = String.valueOf(m.getOrDefault("remark", ""));
                            inputsTable.append("| ").append(fName).append(" | ").append(type).append(" | ").append(req).append(" | ").append(remark).append(" |\n");
                            hasInputs = true;
                        }
                    }
                }
            } catch (Exception e) {
                markdown = "# 解析文档元数据失败\n```\n" + e.getMessage() + "\n```";
            }
        }

        if (markdown.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("# 接口文档: ").append(name != null ? name : "").append(" (").append(bizCode).append(")\n\n");
            sb.append("* **请求方式**: `POST`\n");
            sb.append("* **联调网关**: `").append(requestUri).append("` *(真实Host请前端根据联调环境拼装)*\n\n");
            
            if (hasHeaders) {
                sb.append("### 1. 请求 Headers\n").append(headersTable).append("\n");
            } else {
                sb.append("### 1. 请求 Headers\n*(无特殊Header要求，通用接口鉴权即可)*\n\n");
            }
            
            if (hasInputs) {
                sb.append("### 2. 请求参数 (Body 中的 params 对象)\n").append(inputsTable).append("\n");
            } else {
                sb.append("### 2. 请求参数 (Body 中的 params 对象)\n*(此接口无需额外参数，或未定义参数Schema)*\n\n");
            }
            
            if (!inputExample.isEmpty()) {
                sb.append("### 3. 请求 Body 示例\n```json\n").append(inputExample).append("\n```\n\n");
            }
            
            if (!outputExample.isEmpty()) {
                sb.append("### 4. 返回结果示例\n```json\n").append(outputExample).append("\n```\n\n");
            }
            
            if (docContent == null || docContent.trim().isEmpty()) {
                sb.append("\n> [!NOTE]\n> 该接口尚未在 IDE 中生成详细文档。后端开发人员可以在 IDE 联调自测成功后，点击右侧的 **「保存为文档示例」** 按钮，系统将自动扫描并持久化接口出入参文档。\n");
            }
            markdown = sb.toString();
        }

        String base64Markdown = java.util.Base64.getEncoder().encodeToString(markdown.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        return "<!DOCTYPE html>\n" +
                "<html lang=\"zh-CN\">\n" +
                "<head>\n" +
                "    <meta charset=\"utf-8\">\n" +
                "    <title>" + (name != null ? name : bizCode) + " - 接口使用文档</title>\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
                "    <link rel=\"stylesheet\" href=\"https://cdn.jsdelivr.net/npm/github-markdown-css@5.2.0/github-markdown.min.css\">\n" +
                "    <script src=\"https://cdn.jsdelivr.net/npm/marked@4.3.0/marked.min.js\"></script>\n" +
                "    <style>\n" +
                "        body {\n" +
                "            box-sizing: border-box;\n" +
                "            min-width: 200px;\n" +
                "            max-width: 980px;\n" +
                "            margin: 0 auto;\n" +
                "            padding: 45px;\n" +
                "        }\n" +
                "        @media (max-width: 767px) {\n" +
                "            body {\n" +
                "                padding: 15px;\n" +
                "            }\n" +
                "        }\n" +
                "        .copy-btn {\n" +
                "            position: absolute;\n" +
                "            right: 10px;\n" +
                "            top: 10px;\n" +
                "            padding: 4px 8px;\n" +
                "            font-size: 12px;\n" +
                "            background-color: #f6f8fa;\n" +
                "            border: 1px solid #d0d7de;\n" +
                "            border-radius: 6px;\n" +
                "            cursor: pointer;\n" +
                "        }\n" +
                "        .copy-btn:hover { background-color: #f3f4f6; }\n" +
                "        .markdown-body pre { position: relative; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body class=\"markdown-body\">\n" +
                "    <div id=\"content\">加载中...</div>\n" +
                "    <script>\n" +
                "        const base64Str = \"" + base64Markdown + "\";\n" +
                "        const rawMarkdown = decodeURIComponent(escape(atob(base64Str)));\n" +
                "        document.getElementById('content').innerHTML = marked.parse(rawMarkdown);\n" +
                "\n" +
                "        document.querySelectorAll('pre').forEach(pre => {\n" +
                "            const code = pre.querySelector('code');\n" +
                "            if (code) {\n" +
                "                const btn = document.createElement('button');\n" +
                "                btn.className = 'copy-btn';\n" +
                "                btn.textContent = '复制';\n" +
                "                btn.addEventListener('click', () => {\n" +
                "                    navigator.clipboard.writeText(code.textContent).then(() => {\n" +
                "                        btn.textContent = '已复制!';\n" +
                "                        setTimeout(() => btn.textContent = '复制', 1500);\n" +
                "                    });\n" +
                "                });\n" +
                "                pre.appendChild(btn);\n" +
                "            }\n" +
                "        });\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
    }
}
