package cn.fengin.groovy.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Groovy Engine 演示应用
 * <p>
 * 最小化 Spring Boot 示例，演示 groovy-engine-spring-boot-starter 的完整接入流程。
 * <p>
 * 默认使用 H2 内存数据库，零配置即可启动。
 * 切换 MySQL：{@code java -jar demo.jar --spring.profiles.active=mysql}
 *
 * @author 凌封 (https://aibook.ren)
 */
@SpringBootApplication
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
