# 快速开始指南

> 适用范围：独立工程 `groovy-engine` 的 `main` 分支、版本 `1.0.0`；2026-11-13按源码核验，本次未运行以下步骤。

开发人员先在独立测试进程中运行示例，确认脚本能按约定输入返回结果，再将引擎集成到实际宿主服务。业务数据、身份、事务及正式接口权限由宿主提供。本文不适用于直接替换工程 `inxvision-iot-platform` 的内嵌Groovy实现。

## 前提条件

- 与已有构建文件 `pom.xml` 兼容的Java和Maven环境；源码目标为Java 8，Spring Boot版本为2.7.18。
- 独立测试数据库；已有示例程序使用H2内存库。接入持久数据库时先核对连接、表结构和数据库兼容性。
- 通过本机环境准备测试密钥变量 `GROOVY_TEST_API_KEY`，不复用正式业务环境凭据。

## 先运行现有示例

在本工程根目录执行以下Windows PowerShell命令：

```powershell
if (-not $env:GROOVY_TEST_API_KEY) { throw '请先配置测试密钥环境变量' }
mvn -DskipTests package
if ($LASTEXITCODE -ne 0) { throw '构建失败，请先处理构建输出' }
java "-Dgroovy.engine.manage.api-key=$env:GROOVY_TEST_API_KEY" -jar groovy-engine-demo/target/groovy-engine-demo-1.0.0.jar
```

示例默认监听8080并装入已有SQL文件 `db/schema.sql`、`db/data.sql`；重新启动会重建内存数据。源码已有Java类 [DemoScriptContextFactory](../groovy-engine-demo/src/main/java/cn/fengin/groovy/demo/DemoScriptContextFactory.java) 和 [DemoGatewayController](../groovy-engine-demo/src/main/java/cn/fengin/groovy/demo/DemoGatewayController.java)，无需先编写下面的宿主扩展片段。服务保持运行，在第二个终端执行文末最小验收。

以下步骤用于集成到另一个宿主应用，而不是重复修改已提供的Demo。

## 第一步：添加 Maven 依赖

在宿主已有Maven构建文件 `pom.xml` 中添加下列依赖；须先确保该版本可从本机或约定制品仓库解析。需要本地安装时，在本工程先执行 `mvn -DskipTests install`，这不代表已发布远端制品。

```xml
<dependency>
    <groupId>cn.fengin</groupId>
    <artifactId>groovy-engine-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 第二步：建表

宿主使用默认Java存储类 `JdbcScriptStorage` 时，需要数据源和数据库表 `sys_groovy_script`。下列为表结构示例，完整当前来源见已有文件 [schema.sql](../groovy-engine-demo/src/main/resources/db/schema.sql)。只在明确的测试数据库准备，不对已有业务库重复初始化示例数据。

```sql
CREATE TABLE IF NOT EXISTS sys_groovy_script (
  id bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  biz_code varchar(128) NOT NULL COMMENT '业务编码（路由标识）',
  name varchar(256) DEFAULT NULL COMMENT '接口名称',
  script_content mediumtext NOT NULL COMMENT 'Groovy 脚本源码',
  version int NOT NULL DEFAULT 1 COMMENT '版本号',
  status tinyint NOT NULL DEFAULT 1 COMMENT '1=启用 0=禁用',
  project_code varchar(64) DEFAULT NULL COMMENT '项目编码',
  category varchar(64) DEFAULT NULL COMMENT '分类标签',
  remark varchar(512) DEFAULT NULL COMMENT '备注',
  doc_content mediumtext DEFAULT NULL COMMENT '接口文档元数据 (JSON格式)',
  create_by varchar(64) DEFAULT NULL,
  create_time datetime DEFAULT CURRENT_TIMESTAMP,
  update_by varchar(64) DEFAULT NULL,
  update_time datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_biz_code (biz_code)
);
```

## 第三步：添加配置

在 `application.yml` 中添加：

```yaml
groovy:
  engine:
    manage:
      api-key: "${GROOVY_MANAGE_API_KEY}"    # 由宿主环境提供
```

## 第四步：实现 IScriptContextFactory

宿主必须提供Java接口 `cn.fengin.groovy.api.IScriptContextFactory` 的实现，返回可修改的上下文Map，并显式注入允许脚本使用的业务能力。框架只额外注入追踪工具字段 `t`，不会自动提供登录用户、Redis或事务模板。下面是扩展片段；Java类 `MyUserService`、`MyOrderService` 是待宿主实现的示意服务，不能直接当成引擎现有类。可运行实现请以 `DemoScriptContextFactory` 为起点。

```java
@Component
public class MyScriptContextFactory implements IScriptContextFactory {
    
    @Resource
    private MyUserService userService;
    
    @Resource
    private MyOrderService orderService;
    
    @Resource
    private ApplicationContext applicationContext;

    @Override
    public Map<String, Object> buildContext(Map<String, Object> params) {
        Map<String, Object> ctx = new HashMap<>();
        
        // ① 正式网关已合并参数；管理调试接口使用请求中的params
        ctx.put("params", params != null ? params : Collections.emptyMap());
        
        // ② 业务 Bean — 脚本中通过 ctx.userService.xxx() 调用
        ctx.put("userService", userService);
        ctx.put("orderService", orderService);
        
        // ③ 灵活获取 Bean（按需）
        Set<String> allowed = new HashSet<>(Arrays.asList("com.myapp.service"));
        ctx.put("getBean", new BeanAccessor(applicationContext, allowed));
        
        // ④ 日志
        ctx.put("log", LoggerFactory.getLogger("GroovyScript"));
        
        return ctx;
    }

    @Override
    public Map<String, List<MethodInfo>> getCompletionData() {
        // 返回可用 Bean 的方法签名 → IDE 代码补全
        Map<String, List<MethodInfo>> data = new LinkedHashMap<>();
        data.put("userService", extractMethods(MyUserService.class));
        data.put("orderService", extractMethods(MyOrderService.class));
        return data;
    }

    private List<MethodInfo> extractMethods(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .map(m -> new MethodInfo(m.getName(),
                        Arrays.stream(m.getParameters())
                                .map(p -> p.getType().getSimpleName() + " " + p.getName())
                                .collect(Collectors.joining(", ")),
                        m.getReturnType().getSimpleName()))
                .collect(Collectors.toList());
    }
}
```

## 第五步：创建业务网关（可选）

需要对外提供业务HTTP接口时，由宿主创建Java控制器并设置业务权限。以下Java类 `BizGatewayController` 为扩展示意，继承已有Java抽象类 `cn.fengin.groovy.spring.web.AbstractGroovyGatewayController`；方法名使用 `executeBiz`，明确调用父类执行方法，避免同名方法递归。

```java
@RestController
@RequestMapping("/api/biz/gw")
public class BizGatewayController extends AbstractGroovyGatewayController {

    @RequestMapping(value = "/{bizCode}", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> executeBiz(@PathVariable String bizCode,
                                     @RequestBody(required = false) Map<String, Object> body,
                                     HttpServletRequest request) {
        return super.execute(bizCode, body, request);
    }
}
```

完成宿主装配并保存启用脚本后，HTTP接口 `POST /api/biz/gw/hello-world` 才可执行该业务编码。示例程序实际提供的是 `GET/POST /api/demo/gw/{bizCode}`；上述自定义地址不会由引擎自动生成。管理密钥只约束管理接口，不能代替业务网关权限。

## 第六步：启动并连接 IDE

启动你的 Spring Boot 应用后：

1. **Groovy Web IDE**：打开浏览器，配置服务器地址（如 `http://localhost:8080`）和 API Key
2. **Groovy Desktop IDE**：在设置中填写服务器地址和 API Key

当前两个IDE的新建模板导入平台Java接口 `com.inxaiot.common.client.groovy.IBusinessScript`，连接本独立引擎时改为 `cn.fengin.groovy.api.IBusinessScript`。保存直接写服务端，运行未保存内容会先保存；没有自动回滚或草稿隔离。单次调试通过后还要验证正式网关。

## 使用 H2 内存数据库（开发环境）

如果你不想配置 MySQL，可以使用 H2：

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <version>1.4.200</version>
    <scope>runtime</scope>
</dependency>
```

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:groovy_engine;DB_CLOSE_DELAY=-1;MODE=MySQL
    driver-class-name: org.h2.Driver
  sql:
    init:
      mode: always
      schema-locations: classpath:db/schema.sql
```

## 自定义存储

如果你不想用 MySQL/H2，可以实现 `IScriptStorage` 接口：

```java
@Component
public class RedisScriptStorage implements IScriptStorage {
    // 实现所有方法...
}
```

存储实现通过Spring条件装配替换默认实现。此扩展不等于自动移除MyBatis映射扫描或数据源装配；如要完全脱离数据库，需要在宿主验证完整启动依赖，不能将上述未实现示意类视为已可用存储。

## 最小验收

以下用例仅调用Demo已有的问候脚本，无业务写入；目标是确认管理调试和正式网关分别可用。第二个PowerShell终端应具有同一测试密钥环境变量：

```powershell
if (-not $env:GROOVY_TEST_API_KEY) { throw '缺少测试密钥' }
$testBaseUrl = 'http://localhost:8080'
$testHeaders = @{ 'X-Groovy-Token' = $env:GROOVY_TEST_API_KEY }
$scripts = Invoke-RestMethod -Method Get -Uri "$testBaseUrl/api/groovy/script/list" -Headers $testHeaders
if ($scripts.code -ne 200) { throw '脚本列表业务返回失败' }
if (-not ($scripts.data | Where-Object { $_.bizCode -eq 'hello-world' })) { throw '未加载示例脚本' }

$testJson = @{ bizCode = 'hello-world'; params = @{ name = '验收' }; track = $true } | ConvertTo-Json -Depth 5
$testBody = [System.Text.Encoding]::UTF8.GetBytes($testJson)
$debugResult = Invoke-RestMethod -Method Post -Uri "$testBaseUrl/api/groovy/script/test" -Headers $testHeaders -ContentType 'application/json; charset=utf-8' -Body $testBody
if ($debugResult.code -ne 200 -or $debugResult.data.greeting -ne 'Hello, 验收!') { throw '调试结果不符合预期' }

$encodedName = [System.Uri]::EscapeDataString('验收')
$gatewayResult = Invoke-RestMethod -Method Get -Uri "$testBaseUrl/api/demo/gw/hello-world?name=$encodedName"
if ($gatewayResult.code -ne 200 -or $gatewayResult.data.greeting -ne 'Hello, 验收!') { throw '正式网关结果不符合预期' }
Write-Output '最小验收通过：脚本列表、调试及示例网关均符合预期'
```

同时检查以下用例。Demo正式网关没有业务登录校验，其通过结果不能作为实际项目的权限验收。

| 用例 | 操作 | 预期与证据 |
| --- | --- | --- |
| 管理鉴权 | 调用脚本列表时不传密钥，再传错误密钥 | 管理启用且已配置密钥时返回HTTP 401；记录状态，不记录真实密钥 |
| 不存在脚本 | 调试请求使用未创建的业务编码 | 业务码非200；不能仅用HTTP状态判断成功 |
| 参数边界 | 对实际需求验证缺失、空值、类型错误和上下限 | 与书面接口契约一致；保存请求示例及断言结果 |
| 保存与编译 | 仅用专属测试脚本保存可识别版本，读回详情并执行；另测编译失败后恢复 | 核对数据库版本、缓存执行及恢复结果，不能只看保存提示 |
| 正式接入 | 使用目标宿主真实用户调用正式网关 | 核对用户、项目、参数合并及返回结构；Demo不覆盖该项 |

交付记录至少包括测试环境、宿主构建版本、脚本编码与保存版本、源码摘要、输入、预期/实际结果及未通过或未执行项。只在共享业务测试环境运行已确认允许的用例。更复杂的事务、多实例和超时用例见[架构验收要求](architecture.md#验收要求)。

## 下一步

- 查看 [架构说明](architecture.md) 了解框架内部设计
- 参考 [groovy-engine-demo](../groovy-engine-demo) 查看完整示例代码
