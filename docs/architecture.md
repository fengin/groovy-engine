# 架构说明

> 作者：凌封 — [https://aibook.ren](https://aibook.ren)（AI全书）

## 架构总览

![Groovy Engine 架构图](architecture.png)

## 模块架构

```
┌─────────────────────────────────────────────────────────┐
│                    Your Spring Boot App                  │
│                                                          │
│  ┌──────────────────┐   ┌──────────────────────────┐    │
│  │ MyContextFactory │   │  MyGatewayController     │    │
│  │ (IScriptContext  │   │  (extends Abstract...    │    │
│  │  Factory impl)   │   │   GatewayController)     │    │
│  └────────┬─────────┘   └──────────┬───────────────┘    │
│           │                         │                    │
├───────────┼─────────────────────────┼────────────────────┤
│           │  groovy-engine-spring-boot-starter            │
│           │                         │                    │
│  ┌────────┴─────────────────────────┴──────────────────┐ │
│  │            Auto Configuration                        │ │
│  │  ┌─────────────────┐  ┌────────────────────────┐    │ │
│  │  │ ManageController │  │ AuthInterceptor+CORS   │    │ │
│  │  │ (CRUD + Test)    │  │ (X-Groovy-Token)       │    │ │
│  │  └─────────────────┘  └────────────────────────┘    │ │
│  │  ┌─────────────────┐  ┌────────────────────────┐    │ │
│  │  │ JdbcScriptStorage│  │ GroovyEngineProperties │    │ │
│  │  │ (MyBatis-Plus)   │  │ (groovy.engine.*)      │    │ │
│  │  └────────┬────────┘  └────────────────────────┘    │ │
│  └───────────┼──────────────────────────────────────────┘ │
│              │                                            │
├──────────────┼────────────────────────────────────────────┤
│              │  groovy-engine-core （纯 Java）              │
│  ┌───────────┴──────────────────────────────────────────┐ │
│  │                                                       │ │
│  │  ┌──────────────┐  ┌────────────────────┐            │ │
│  │  │ ScriptManager │  │ScriptExecutionSvc  │            │ │
│  │  │ (编译+缓存)    │  │(执行门面+超时控制)   │            │ │
│  │  └──────────────┘  └────────────────────┘            │ │
│  │                                                       │ │
│  │  ┌──────────────┐  ┌─────────────┐ ┌──────────────┐ │ │
│  │  │ BeanAccessor  │  │ScriptTracer │ │IScriptStorage│ │ │
│  │  │ (安全沙箱)     │  │(追踪工具)    │ │(存储抽象)     │ │ │
│  │  └──────────────┘  └─────────────┘ └──────────────┘ │ │
│  │                                                       │ │
│  │  ┌──────────────────────────────────────────────────┐ │ │
│  │  │ IBusinessScript | IScriptContextFactory (SPI)    │ │ │
│  │  └──────────────────────────────────────────────────┘ │ │
│  └───────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## 核心执行流程

```
HTTP Request
    │
    ▼
GatewayController / ManageController
    │
    ▼
ScriptExecutionService.execute(bizCode, params, track)
    │
    ├─ 1. ScriptManager.getScript(bizCode)     → 从缓存获取已编译实例
    │     └─ 缓存未命中 → IScriptStorage.getByBizCode() → 编译 → 缓存
    │
    ├─ 2. IScriptContextFactory.buildContext()  → 构建执行上下文
    │
    ├─ 3. 注入 ScriptTracer + 可选 track 代理   → 追踪能力
    │
    ├─ 4. 调度超时看门狗                         → 线程中断保护
    │
    └─ 5. script.execute(context)               → 执行 Groovy 脚本
          │
          └─ 返回 ScriptResult { code, data, _trace, _track, cost }
```

## ClassLoader 生命周期

```
启动 → 创建 GroovyClassLoader → 预加载所有脚本
  │
  ├─ 单个刷新 → 移除缓存 → 重新编译 → 累计计数+1
  │
  ├─ 达到阈值(100) → 新建 ClassLoader → 迁移全部 → 关闭旧 ClassLoader → GC
  │
  └─ 全量重载 → 同上（强制回收）
```

## 安全机制

### BeanAccessor 白名单

脚本通过 `ctx.getBean.get("beanName")` 获取 Bean 时，BeanAccessor 会检查 Bean 所在的包名是否在白名单中：

```java
Set<String> allowed = new HashSet<>(Arrays.asList("com.myapp.service"));
ctx.put("getBean", new BeanAccessor(applicationContext, allowed));
```

非白名单包的 Bean（如 DataSource、TransactionManager）会被拒绝访问。

### 超时保护

通过 Groovy AST 转换 `@ThreadInterrupt` 在编译阶段注入中断检查点，脚本中的循环和方法调用处自动检查 `Thread.isInterrupted()`。超时后框架中断脚本执行线程。

### API Key 鉴权

所有管理接口（`/api/groovy/script/**`）通过 `X-Groovy-Token` Header 进行 API Key 鉴权。业务网关接口可由各项目自行添加权限控制。

## 扩展点

| 接口 | 模块 | 说明 | 是否必须实现 |
|------|------|------|:---:|
| `IScriptContextFactory` | core | 定义脚本执行上下文 | ✅ 必须 |
| `IScriptStorage` | core | 脚本存储抽象 | ❌ 默认 JDBC |
| `AbstractGroovyGatewayController` | starter | 业务网关基类 | ❌ 可选 |
