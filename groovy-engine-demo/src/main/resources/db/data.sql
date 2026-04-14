-- 示例脚本：Hello World
INSERT INTO sys_groovy_script (biz_code, name, script_content, version, status, category, remark) VALUES
('hello-world', 'Hello World', 'import cn.fengin.groovy.api.IBusinessScript

class HelloWorldScript implements IBusinessScript {
    Object execute(Map<String, Object> ctx) {
        def t = ctx.t
        def params = ctx.params

        t.log("Hello World 脚本开始执行")

        def name = params?.name ?: "World"
        t.log("参数 name={}", name)

        return [
            greeting: "Hello, ${name}!".toString(),
            timestamp: new Date().format("yyyy-MM-dd HH:mm:ss"),
            message: "欢迎使用 Groovy Engine！"
        ]
    }
}', 1, 1, 'demo', '最简单的 Hello World 示例');

-- 示例脚本：调用业务 Bean
INSERT INTO sys_groovy_script (biz_code, name, script_content, version, status, category, remark) VALUES
('demo-query', '数据查询示例', 'import cn.fengin.groovy.api.IBusinessScript

class DemoQueryScript implements IBusinessScript {
    Object execute(Map<String, Object> ctx) {
        def t = ctx.t
        def params = ctx.params
        def demoService = ctx.demoService

        t.log("开始查询数据")

        // 调用 Java Bean 方法
        def category = params?.category ?: "sensor"
        def data = demoService.queryData(category)
        t.log("查询到 {} 条数据", data.size())

        // 调用计算方法
        def total = new BigDecimal(0)
        data.each { row -> total = total.add(new BigDecimal(row.value)) }
        t.log("合计值: {}", total)

        return [
            category: category,
            items: data,
            total: total,
            queryTime: demoService.now("yyyy-MM-dd HH:mm:ss")
        ]
    }
}', 1, 1, 'demo', '演示如何调用 Java Bean 和参数传递');

-- 示例脚本：参数回显
INSERT INTO sys_groovy_script (biz_code, name, script_content, version, status, category, remark) VALUES
('echo-params', '参数回显', 'import cn.fengin.groovy.api.IBusinessScript

class EchoParamsScript implements IBusinessScript {
    Object execute(Map<String, Object> ctx) {
        def t = ctx.t
        def params = ctx.params

        t.log("收到参数个数: {}", params?.size() ?: 0)
        params?.each { k, v ->
            if (!k.startsWith("_")) {
                t.log("  {} = {}", k, v)
            }
        }

        return [
            receivedParams: params?.findAll { k, v -> !k.startsWith("_") },
            method: params?._method,
            uri: params?._uri,
            timestamp: new Date().format("yyyy-MM-dd HH:mm:ss")
        ]
    }
}', 1, 1, 'demo', '回显所有传入参数，调试用');

-- 示例脚本：文件下载（返回 byte[]）
INSERT INTO sys_groovy_script (biz_code, name, script_content, version, status, category, remark) VALUES
('file-download', '文件下载示例', 'import cn.fengin.groovy.api.IBusinessScript

class FileDownloadScript implements IBusinessScript {
    Object execute(Map<String, Object> ctx) {
        def t = ctx.t
        def params = ctx.params

        def format = params?.format ?: "csv"
        t.log("生成 {} 格式示例文件...", format)

        // 构造 CSV 内容
        def sb = new StringBuilder()
        sb.append("id,name,value,time\n")
        def demoService = ctx.demoService
        def data = demoService.queryData(params?.category ?: "sensor")
        data.each { row ->
            sb.append("${row.id},${row.name},${row.value},${row.time}\n")
        }
        t.log("生成 {} 行数据", data.size())

        // 返回 byte[] → 框架自动切换为文件下载响应
        return sb.toString().getBytes("UTF-8")
    }
}', 1, 1, 'demo', '演示脚本返回 byte[] 触发文件下载');

-- 示例脚本：文件下载异常（验证异常时不返回文件流）
INSERT INTO sys_groovy_script (biz_code, name, script_content, version, status, category, remark) VALUES
('file-download-error', '文件下载异常示例', 'import cn.fengin.groovy.api.IBusinessScript
import cn.fengin.groovy.exception.GroovyEngineException

class FileDownloadErrorScript implements IBusinessScript {
    Object execute(Map<String, Object> ctx) {
        def t = ctx.t
        t.log("准备生成文件...")

        // 模拟业务校验失败
        throw new GroovyEngineException("参数缺失：请传入 year 参数")
    }
}', 1, 1, 'demo', '演示文件下载脚本抛异常时返回 JSON 而非文件流');
