# Groovy Engine

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-8%2B-orange.svg)](https://openjdk.java.net)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.x-green.svg)](https://spring.io/projects/spring-boot)

一个轻量级的 **Groovy 动态脚本引擎框架**，用于在 Java 应用中执行动态业务逻辑。提供脚本管理、编译缓存、执行追踪、代码补全等完整能力，并配有专属 Web IDE 和 Desktop IDE 进行脚本开发调试。

## ✨ 特性

- **动态热加载** — 脚本修改立即生效，无需重启服务
- **编译缓存** — 智能 ClassLoader 管理，避免重复编译开销
- **执行追踪** — 内置 trace/track 模式，调试脚本如同本地调试
- **代码补全** — IDE 可获取注入 Bean 的方法签名，提供智能提示
- **安全沙箱** — BeanAccessor 白名单机制，防止脚本越权访问
- **超时保护** — AST 级线程中断，防止死循环脚本拖垮服务
- **Spring Boot 一键集成** — Starter 自动装配，三步接入
- **存储可扩展** — 默认 MySQL/H2，可自定义实现 Redis、文件等存储
- **IDE 支持** — 适配 [Groovy Web IDE](https://github.com/user/groovy-web-ide) 和 [Groovy Desktop IDE](https://github.com/user/groovy-desktop-ide)

## 🏗 架构图

![Groovy Engine 架构图](docs/architecture.png)

## 📦 模块结构

```
groovy-engine/
├── groovy-engine-core                 # 核心引擎（纯 Java，无 Spring 依赖）
├── groovy-engine-spring-boot-starter  # Spring Boot 自动配置 + REST API
└── groovy-engine-demo                 # 演示项目（零配置启动）
```

## 🚀 快速开始

### 1. 运行 Demo

```bash
git clone https://github.com/fengin/groovy-engine.git
cd groovy-engine
mvn clean package -DskipTests
java -jar groovy-engine-demo/target/groovy-engine-demo-1.0.0.jar
```

Demo 使用 H2 内存数据库，零配置启动。启动后：
- 打开 Web IDE，配置连接 `http://localhost:8080`，API Key 为 `demo-key-123`
- 或直接调用 API：`curl -H "X-Groovy-Token: demo-key-123" http://localhost:8080/api/groovy/script/list`

### 2. 集成到你的项目

**Step 1 — 添加依赖**

```xml
<dependency>
    <groupId>cn.fengin</groupId>
    <artifactId>groovy-engine-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Step 2 — 添加配置**

```yaml
groovy:
  engine:
    manage:
      api-key: "your-secret-key"    # IDE 管理接口鉴权密钥
```

**Step 3 — 实现上下文工厂**

```java
@Component
public class MyScriptContextFactory implements IScriptContextFactory {
    
    @Resource
    private MyQueryService queryService;

    @Override
    public Map<String, Object> buildContext(Map<String, Object> params) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("params", params);
        ctx.put("queryService", queryService);
        ctx.put("log", LoggerFactory.getLogger("GroovyScript"));
        return ctx;
    }
}
```

**Step 4 — 创建业务网关（可选）**

```java
@RestController
@RequestMapping("/api/my/gw")
public class MyGatewayController extends AbstractGroovyGatewayController {

    @RequestMapping(value = "/{bizCode}", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> execute(@PathVariable String bizCode,
                                     @RequestBody(required = false) Map<String, Object> body,
                                     HttpServletRequest request) {
        return execute(bizCode, body, request);
    }
}
```

搞定！IDE 连接你的服务就可以开始写 Groovy 脚本了。

## 📝 编写 Groovy 脚本

所有脚本必须实现 `IBusinessScript` 接口：

```groovy
import cn.fengin.groovy.api.IBusinessScript

class MyScript implements IBusinessScript {
    Object execute(Map<String, Object> ctx) {
        def t = ctx.t              // 追踪工具：t.log("消息")
        def params = ctx.params    // 请求参数
        def queryService = ctx.queryService  // 注入的 Bean
        
        t.log("开始执行业务逻辑")
        
        def result = queryService.query(params.id)
        t.log("查询结果: {}", result)
        
        return [status: "ok", data: result]
    }
}
```

## ⚙️ 配置项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `groovy.engine.enabled` | `true` | 引擎总开关 |
| `groovy.engine.manage.api-key` | `""` | IDE 管理接口鉴权密钥（必填） |
| `groovy.engine.manage.enabled` | `true` | 管理接口开关 |
| `groovy.engine.script.timeout-seconds` | `600` | 脚本执行超时（秒），0 表示不限制 |
| `groovy.engine.classloader.recycle-threshold` | `100` | ClassLoader 回收阈值 |

## 🗄️ 数据库

默认使用 `sys_groovy_script` 表存储脚本，建表 SQL 见 [schema.sql](groovy-engine-demo/src/main/resources/db/schema.sql)。

支持 MySQL 和 H2。也可以实现 `IScriptStorage` 接口自定义存储。

### 切换 MySQL

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/your_db?characterEncoding=utf8mb4
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: root
    password: your-password
```

## 🌐 API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/groovy/script/list` | 脚本列表 |
| GET | `/api/groovy/script/{id}` | 脚本详情 |
| POST | `/api/groovy/script` | 新建脚本 |
| PUT | `/api/groovy/script/{id}` | 更新脚本 |
| DELETE | `/api/groovy/script/{id}` | 删除脚本 |
| POST | `/api/groovy/script/test` | 测试执行 |
| POST | `/api/groovy/script/deploy` | 批量部署 |
| GET | `/api/groovy/script/completions` | 代码补全 |
| POST | `/api/groovy/script/refresh/{bizCode}` | 刷新缓存 |
| POST | `/api/groovy/script/refresh/all` | 刷新全部 |

所有管理接口需要 `X-Groovy-Token` Header。

## 🛠 技术栈

- Java 8+
- Groovy 3.0.22
- Spring Boot 2.x
- MyBatis-Plus 3.5.x（默认存储）

## 📄 License

[Apache License, Version 2.0](LICENSE)

## 👨‍💻 作者

**凌封** — [https://aibook.ren](https://aibook.ren)（AI全书）

技术交流圈：[https://aibook.ren](https://aibook.ren)
