package com.cps.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 应用启动入口。
 *
 * <p>Data JDBC 自动配置的排除通过 {@code application-test.yaml} 的
 * {@code spring.autoconfigure.exclude} 配置完成（参考 02-Data-Dictionary.md §1.3）。</p>
 */
@SpringBootApplication
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

}
