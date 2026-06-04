package com.cps.backend.modules.M01userauth.service;

import com.cps.backend.common.exception.BusinessException;
import com.cps.backend.modules.M01userauth.dto.*;
import com.cps.backend.modules.M01userauth.entity.User;
import com.cps.backend.modules.M01userauth.enums.UserType;
import com.cps.backend.modules.M01userauth.repository.UserRepository;
import com.cps.backend.modules.M03examassembly.repository.ExamRepository;
import com.cps.backend.modules.M04scorestatistics.repository.ScoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private ScoreRepository scoreRepository;

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
        // 创建初始admin（绕过注册角色限制，直接通过 Repository 创建）
        User adminEntity = new User();
        adminEntity.setName("onlyadmin");
        adminEntity.setPassword(new BCryptPasswordEncoder().encode("password123"));
        adminEntity.setType(UserType.admin);
        adminEntity.setStatus(1);
        User savedAdmin = userRepository.save(adminEntity);
        UserStatusReq statusReq = new UserStatusReq(0);
        BusinessException ex = assertThrows(BusinessException.class,
            () -> userService.updateStatus(savedAdmin.getId(), statusReq));
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
        // 创建admin（绕过注册角色限制，直接通过 Repository 创建）
        User adminEntity = new User();
        adminEntity.setName("lastadmin");
        adminEntity.setPassword(new BCryptPasswordEncoder().encode("password123"));
        adminEntity.setType(UserType.admin);
        adminEntity.setStatus(1);
        User savedAdmin = userRepository.save(adminEntity);
        assertThrows(BusinessException.class, () -> userService.delete(savedAdmin.getId(), 999));
    }

    @Test
    @DisplayName("按ID查询用户")
    void findByIdSuccess() {
        // 创建teacher（绕过注册角色限制，直接通过 Repository 创建）
        User teacherEntity = new User();
        teacherEntity.setName("finduser");
        teacherEntity.setPassword(new BCryptPasswordEncoder().encode("password123"));
        teacherEntity.setType(UserType.teacher);
        teacherEntity.setStatus(1);
        User savedTeacher = userRepository.save(teacherEntity);
        UserVO found = userService.findById(savedTeacher.getId());
        assertEquals("finduser", found.name());
    }

    @Test
    @DisplayName("按ID查询 - 用户不存在")
    void findByIdNotFound() {
        assertThrows(BusinessException.class, () -> userService.findById(99999));
    }

    @Test
    @DisplayName("删除有考试记录的用户 - 禁用而非删除")
    void deleteUserWithScoreRecordsDisablesInsteadOfDelete() {
        // 注册用户
        UserVO user = userService.register(new RegisterReq("scoreuser", "password123", UserType.student));

        // 创建考试和分数记录（需要通过其他 Service 创建）
        // 由于跨模块依赖，直接通过 ScoreRepository 创建测试数据
        // 先创建考试
        com.cps.backend.modules.M03examassembly.entity.Exam exam = new com.cps.backend.modules.M03examassembly.entity.Exam();
        exam.setExam("测试考试");
        exam.setStatus(com.cps.backend.modules.M03examassembly.enums.ExamStatus.done);
        exam.setStarttime("2099-01-01T09:00:00");
        exam.setEndtime("2099-01-01T11:00:00");
        exam.setQuestionSum("{\"version\":1,\"items\":[],\"totalQuestions\":0,\"totalScore\":0}");
        examRepository.save(exam);

        // 创建分数记录
        com.cps.backend.modules.M04scorestatistics.entity.Score score = new com.cps.backend.modules.M04scorestatistics.entity.Score();
        score.setUser(user.id());
        score.setExam(exam.getId());
        score.setAll(80);
        score.setDetail("{\"version\":1,\"items\":[],\"summary\":{\"correctCount\":0,\"totalCount\":0,\"accuracy\":0}}");
        scoreRepository.save(score);

        // 尝试删除有分数记录的用户
        assertThatThrownBy(() -> userService.delete(user.id(), 999))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(4103));

        // 验证用户被禁用而非删除
        UserVO disabledUser = userService.findById(user.id());
        assertThat(disabledUser.status()).isEqualTo(0);
    }

    @Test
    @DisplayName("注册失败 - 教师角色被拒绝")
    void registerTeacherRejected() {
        assertThatThrownBy(() -> userService.register(new RegisterReq("teacher1", "password123", UserType.teacher)))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(4103));
    }

    @Test
    @DisplayName("注册失败 - 管理员角色被拒绝")
    void registerAdminRejected() {
        assertThatThrownBy(() -> userService.register(new RegisterReq("admin1", "password123", UserType.admin)))
            .isInstanceOf(BusinessException.class)
            .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo(4103));
    }
}
