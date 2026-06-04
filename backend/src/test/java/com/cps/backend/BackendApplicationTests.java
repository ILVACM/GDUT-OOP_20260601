package com.cps.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 上下文加载冒烟测试。
 *
 * <p>使用 {@code test} profile 切换到测试 SQLite（{@code target/test-data/test.sqlite}），
 * 避免对生产 {@code Data/English.sqlite} 产生依赖（参考 SQLite-Optimization.md §2）。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class BackendApplicationTests {

    @Test
    void contextLoads() {
    }

}
