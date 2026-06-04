package com.cps.backend.modules.M01userauth.repository;

import com.cps.backend.modules.M01userauth.entity.User;
import com.cps.backend.modules.M01userauth.enums.UserType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * User 实体的 JPA Repository。
 *
 * <p>参考 02-Data-Dictionary.md §4.1、M01-User-Auth。</p>
 *
 * <p>v2.0.0 4 张核心表均无外键关联（参考 02-Data-Dictionary.md §2 ER 图），
 * 故 {@link EntityGraph#attributePaths()} 为空，零 N+1 风险（参考 01-Global-Standards.md §4.4）。</p>
 *
 * <p>命名遵循 Spring Data 规范（{@code findByXxx} / {@code existsByXxx}，
 * 参考 01-Global-Standards.md §4.1 J3 + 02-Data-Dictionary.md §10.1 M7）。</p>
 */
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    /**
     * 按用户名精确查询。
     * <p>主要用于登录校验（参考 §7.1 业务矩阵）。</p>
     */
    @EntityGraph(attributePaths = {})
    Optional<User> findByName(String name);

    /**
     * 判断用户名是否存在。
     * <p>用于注册时前置校验，避免 UNIQUE 冲突异常。</p>
     */
    @EntityGraph(attributePaths = {})
    boolean existsByName(String name);

    /**
     * 按角色与状态筛选用户（管理员视角）。
     * <p>对应 §7.1 列表/分页查询。</p>
     */
    @EntityGraph(attributePaths = {})
    List<User> findByTypeAndStatus(UserType type, Integer status);

    /**
     * 按角色筛选用户。
     */
    @EntityGraph(attributePaths = {})
    List<User> findByType(UserType type);
}
