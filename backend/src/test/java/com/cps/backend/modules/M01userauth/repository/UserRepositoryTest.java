package com.cps.backend.modules.M01userauth.repository;

import com.cps.backend.modules.M01userauth.entity.User;
import com.cps.backend.modules.M01userauth.enums.UserType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link UserRepository} 单元测试。
 *
 * <p>使用真实 SQLite（{@code test} profile，{@code ddl-auto=create-drop}）。</p>
 *
 * <p>参考 02-Data-Dictionary.md §4.1、§7.1。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User newUser(String name, UserType type, Integer status) {
        User u = new User();
        u.setName(name);
        // 测试用 BCrypt 哈希样例（应用层负责加密，此处仅模拟）
        u.setPassword("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
        u.setType(type);
        u.setStatus(status);
        return u;
    }

    @Test
    void saveAndFindById() {
        User saved = userRepository.save(newUser("alice", UserType.student, 1));
        assertNotNull(saved.getId());

        Optional<User> found = userRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("alice", found.get().getName());
        assertEquals(UserType.student, found.get().getType());
        assertEquals(Integer.valueOf(1), found.get().getStatus());
    }

    @Test
    void findByName() {
        userRepository.save(newUser("bob", UserType.teacher, 1));

        Optional<User> found = userRepository.findByName("bob");
        assertTrue(found.isPresent());
        assertEquals(UserType.teacher, found.get().getType());

        assertTrue(userRepository.findByName("nobody").isEmpty());
    }

    @Test
    void existsByName() {
        userRepository.save(newUser("charlie", UserType.admin, 1));
        assertTrue(userRepository.existsByName("charlie"));
        assertFalse(userRepository.existsByName("nobody"));
    }

    @Test
    void findByTypeAndStatus() {
        userRepository.save(newUser("d1", UserType.student, 1));
        userRepository.save(newUser("d2", UserType.student, 0));
        userRepository.save(newUser("d3", UserType.teacher, 1));

        List<User> activeStudents = userRepository.findByTypeAndStatus(UserType.student, 1);
        assertEquals(1, activeStudents.size());
        assertEquals("d1", activeStudents.get(0).getName());
    }

    @Test
    void uniqueNameConstraint() {
        // DB UNIQUE 约束由 idx_user_name 强制（参考 §4.1 索引）
        userRepository.save(newUser("duplicate", UserType.student, 1));
        // 同一事务内第二条同 name 写入应触发 UNIQUE 冲突（Hibernate 抛 JpaSystemException）
        assertThrows(Exception.class,
                () -> userRepository.saveAndFlush(newUser("duplicate", UserType.teacher, 1)));
    }

    @Test
    void enumStringRoundTrip() {
        // 验证 @Enumerated(EnumType.STRING) 写入为字面量，读出为枚举
        userRepository.save(newUser("enum_test", UserType.admin, 1));
        Optional<User> found = userRepository.findByName("enum_test");
        assertTrue(found.isPresent());
        assertEquals(UserType.admin, found.get().getType());
    }
}
