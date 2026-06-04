package com.cps.backend.modules.M01userauth.service;

import com.cps.backend.common.exception.BusinessException;
import com.cps.backend.modules.M01userauth.dto.*;
import com.cps.backend.modules.M01userauth.enums.UserType;
import com.cps.backend.modules.M01userauth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

// 参考 M01-User-Auth.md §7 业务规则
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanUp() {
        // 清理测试数据
    }

    @Test
    @DisplayName("注册成功")
    void registerSuccess() {
        RegisterReq req = new RegisterReq("testuser", "password123", UserType.student);
        UserVO vo = userService.register(req);
        assertNotNull(vo.id());
        assertEquals("testuser", vo.name());
        assertEquals(UserType.student, vo.type());
        assertEquals(1, vo.status());
    }

    @Test
    @DisplayName("注册失败 - 用户名重复")
    void registerDuplicateName() {
        RegisterReq req1 = new RegisterReq("dupuser", "password123", UserType.student);
        userService.register(req1);
        RegisterReq req2 = new RegisterReq("dupuser", "password456", UserType.teacher);
        assertThrows(BusinessException.class, () -> userService.register(req2));
    }

    @Test
    @DisplayName("登录成功")
    void loginSuccess() {
        RegisterReq req = new RegisterReq("loginuser", "password123", UserType.student);
        userService.register(req);
        LoginReq loginReq = new LoginReq("loginuser", "password123");
        LoginResp resp = userService.login(loginReq);
        assertNotNull(resp.user());
        assertEquals("loginuser", resp.user().name());
    }

    @Test
    @DisplayName("登录失败 - 密码错误")
    void loginWrongPassword() {
        RegisterReq req = new RegisterReq("loginuser2", "password123", UserType.student);
        userService.register(req);
        LoginReq loginReq = new LoginReq("loginuser2", "wrongpassword");
        assertThrows(BusinessException.class, () -> userService.login(loginReq));
    }

    @Test
    @DisplayName("登录失败 - 用户被禁用")
    void loginDisabledUser() {
        RegisterReq req = new RegisterReq("disableduser", "password123", UserType.student);
        UserVO vo = userService.register(req);
        userService.updateStatus(vo.id(), new UserStatusReq(0));
        LoginReq loginReq = new LoginReq("disableduser", "password123");
        BusinessException ex = assertThrows(BusinessException.class, () -> userService.login(loginReq));
        assertEquals(4102, ex.getCode());
    }

    @Test
    @DisplayName("修改密码成功")
    void changePasswordSuccess() {
        RegisterReq req = new RegisterReq("pwduser", "oldpassword", UserType.student);
        UserVO vo = userService.register(req);
        ChangePasswordReq cpReq = new ChangePasswordReq("oldpassword", "newpassword");
        assertDoesNotThrow(() -> userService.changePassword(vo.id(), cpReq));
        // 用新密码登录
        LoginReq loginReq = new LoginReq("pwduser", "newpassword");
        assertDoesNotThrow(() -> userService.login(loginReq));
    }

    @Test
    @DisplayName("修改密码失败 - 旧密码错误")
    void changePasswordWrongOld() {
        RegisterReq req = new RegisterReq("pwduser2", "oldpassword", UserType.student);
        UserVO vo = userService.register(req);
        ChangePasswordReq cpReq = new ChangePasswordReq("wrongold", "newpassword");
        assertThrows(BusinessException.class, () -> userService.changePassword(vo.id(), cpReq));
    }

    @Test
    @DisplayName("状态启停 - 最后一个admin保护")
    void cannotDisableLastAdmin() {
        // 创建初始admin
        RegisterReq req = new RegisterReq("onlyadmin", "password123", UserType.admin);
        UserVO vo = userService.register(req);
        UserStatusReq statusReq = new UserStatusReq(0);
        BusinessException ex = assertThrows(BusinessException.class,
            () -> userService.updateStatus(vo.id(), statusReq));
        assertEquals(4103, ex.getCode());
    }

    @Test
    @DisplayName("删除用户 - 自删除保护")
    void cannotDeleteSelf() {
        RegisterReq req = new RegisterReq("selfdelete", "password123", UserType.student);
        UserVO vo = userService.register(req);
        assertThrows(BusinessException.class, () -> userService.delete(vo.id(), vo.id()));
    }

    @Test
    @DisplayName("删除用户 - 最后一个admin保护")
    void cannotDeleteLastAdmin() {
        RegisterReq req = new RegisterReq("lastadmin", "password123", UserType.admin);
        UserVO vo = userService.register(req);
        assertThrows(BusinessException.class, () -> userService.delete(vo.id(), 999));
    }

    @Test
    @DisplayName("按ID查询用户")
    void findByIdSuccess() {
        RegisterReq req = new RegisterReq("finduser", "password123", UserType.teacher);
        UserVO created = userService.register(req);
        UserVO found = userService.findById(created.id());
        assertEquals(created.name(), found.name());
    }

    @Test
    @DisplayName("按ID查询 - 用户不存在")
    void findByIdNotFound() {
        assertThrows(BusinessException.class, () -> userService.findById(99999));
    }
}
