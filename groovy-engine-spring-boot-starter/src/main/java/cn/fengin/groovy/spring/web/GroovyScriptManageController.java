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
    public GroovyApiResult<Void> update(@PathVariable Long id, @RequestBody GroovyScript script) {
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
        scriptManager.refreshScript(existing.getBizCode());
        return GroovyApiResult.success(null);
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
}
