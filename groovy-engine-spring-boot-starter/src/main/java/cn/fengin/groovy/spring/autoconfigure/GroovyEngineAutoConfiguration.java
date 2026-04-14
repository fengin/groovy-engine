package cn.fengin.groovy.spring.autoconfigure;

import cn.fengin.groovy.api.IScriptContextFactory;
import cn.fengin.groovy.api.IScriptStorage;
import cn.fengin.groovy.engine.ScriptExecutionService;
import cn.fengin.groovy.engine.ScriptManager;
import cn.fengin.groovy.spring.storage.GroovyScriptMapper;
import cn.fengin.groovy.spring.storage.JdbcScriptStorage;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Groovy Engine Spring Boot 自动装配
 *
 * @author 凌封 (https://aibook.ren)
 */
@Configuration
@ConditionalOnProperty(prefix = "groovy.engine", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(GroovyEngineProperties.class)
@ComponentScan(basePackages = "cn.fengin.groovy.spring")
@MapperScan("cn.fengin.groovy.spring.storage")
public class GroovyEngineAutoConfiguration {

    /**
     * 默认 JDBC 存储实现（业务方可通过自定义 IScriptStorage Bean 覆盖）
     */
    @Bean
    @ConditionalOnMissingBean(IScriptStorage.class)
    public IScriptStorage scriptStorage(GroovyScriptMapper mapper) {
        return new JdbcScriptStorage(mapper);
    }

    /**
     * 脚本编译+缓存管理器
     */
    @Bean
    public ScriptManager scriptManager(IScriptStorage storage, GroovyEngineProperties props) {
        return new ScriptManager(
                storage,
                props.getClassloader().getRecycleThreshold(),
                props.getScript().getTimeoutSeconds()
        );
    }

    /**
     * 脚本执行门面（需要业务方提供 IScriptContextFactory 实现）
     */
    @Bean
    @ConditionalOnBean(IScriptContextFactory.class)
    public ScriptExecutionService scriptExecutionService(ScriptManager scriptManager,
                                                         IScriptContextFactory contextFactory) {
        return new ScriptExecutionService(scriptManager, contextFactory);
    }
}
