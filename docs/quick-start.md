# 快速开始指南

本指南帮助你在 10 分钟内将 Groovy Engine 集成到你的 Spring Boot 项目中。

## 前提条件

- JDK 1.8+
- Maven 3.x
- MySQL 5.7+（或使用 H2 内存数据库）

## 第一步：添加 Maven 依赖

在你的 `pom.xml` 中添加：

```xml
<dependency>
    <groupId>cn.fengin</groupId>
    <artifactId>groovy-engine-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 第二步：建表

在你的数据库中执行以下 SQL：

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
      api-key: "your-secret-key"    # IDE 连接密钥，务必修改
```

## 第四步：实现 IScriptContextFactory

这是**唯一必须实现的接口**。它定义了 Groovy 脚本可以访问的上下文内容。

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
        
        // ① 请求参数（框架已合并 query params + body）
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

如果你需要通过 HTTP 接口调用脚本（而非仅在 IDE 中测试），创建一个网关 Controller：

```java
@RestController
@RequestMapping("/api/biz/gw")
public class BizGatewayController extends AbstractGroovyGatewayController {

    @RequestMapping(value = "/{bizCode}", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<?> execute(@PathVariable String bizCode,
                                     @RequestBody(required = false) Map<String, Object> body,
                                     HttpServletRequest request) {
        return execute(bizCode, body, request);
    }
}
```

访问 `POST /api/biz/gw/hello-world` 就会执行 bizCode 为 `hello-world` 的脚本。

## 第六步：启动并连接 IDE

启动你的 Spring Boot 应用后：

1. **Groovy Web IDE**：打开浏览器，配置服务器地址（如 `http://localhost:8080`）和 API Key
2. **Groovy Desktop IDE**：在设置中填写服务器地址和 API Key

连接成功后即可在 IDE 中管理和测试脚本。

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

框架会自动使用你的自定义实现（`@ConditionalOnMissingBean` 机制）。

## 下一步

- 查看 [架构说明](architecture.md) 了解框架内部设计
- 参考 [groovy-engine-demo](../groovy-engine-demo) 查看完整示例代码
