package com.cps.backend.modules.M01userauth.controller;

import com.cps.backend.common.api.PageResult;
import com.cps.backend.common.api.Result;
import com.cps.backend.common.security.JwtUtil;
import com.cps.backend.common.security.RequireRole;
import com.cps.backend.modules.M01userauth.dto.*;
import com.cps.backend.modules.M01userauth.enums.UserType;
import com.cps.backend.modules.M01userauth.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    // POST /api/v1/auth/login - 公开接口，无需登录
    @PostMapping("/auth/login")
    public Result<LoginResp> login(@Valid @RequestBody LoginReq req) {
        LoginResp resp = userService.login(req);
        // 签发 JWT Token
        String token = jwtUtil.generateToken(resp.user().id(), resp.user().name(), resp.user().type().name());
        return Result.success(new LoginResp(token, resp.user()));
    }

    // POST /api/v1/auth/register - 公开接口，无需登录
    @PostMapping("/auth/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterReq req) {
        return Result.success(userService.register(req));
    }

    // POST /api/v1/auth/logout - JWT 无状态，直接返回成功
    @PostMapping("/auth/logout")
    @RequireRole({UserType.student, UserType.teacher, UserType.admin})
    public Result<Void> logout() {
        return Result.success();
    }

    // GET /api/v1/auth/me - 获取当前用户信息
    @GetMapping("/auth/me")
    @RequireRole({UserType.student, UserType.teacher, UserType.admin})
    public Result<UserVO> getCurrentUser(HttpServletRequest request) {
        Integer userId = (Integer) request.getAttribute("userId");
        return Result.success(userService.getCurrentUser(userId));
    }

    // POST /api/v1/auth/password - 修改密码
    @PostMapping("/auth/password")
    @RequireRole({UserType.student, UserType.teacher, UserType.admin})
    public Result<Void> changePassword(HttpServletRequest request, @Valid @RequestBody ChangePasswordReq req) {
        Integer userId = (Integer) request.getAttribute("userId");
        userService.changePassword(userId, req);
        return Result.success();
    }

    // GET /api/v1/users - 管理员分页查询用户列表
    @GetMapping("/users")
    @RequireRole(UserType.admin)
    public Result<PageResult<UserVO>> listUsers(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) UserType type,
            @RequestParam(required = false) Integer status) {
        return Result.success(userService.listUsers(page, size, type, status));
    }

    // POST /api/v1/users - 管理员创建用户
    @PostMapping("/users")
    @RequireRole(UserType.admin)
    public Result<UserVO> createUser(@Valid @RequestBody RegisterReq req) {
        return Result.success(userService.createUser(req));
    }

    // PUT /api/v1/users/{id} - 管理员更新用户
    @PutMapping("/users/{id}")
    @RequireRole(UserType.admin)
    public Result<UserVO> updateUser(@PathVariable Integer id, @Valid @RequestBody RegisterReq req) {
        return Result.success(userService.updateUser(id, req));
    }

    // PATCH /api/v1/users/{id}/status - 管理员更新用户状态
    @PatchMapping("/users/{id}/status")
    @RequireRole(UserType.admin)
    public Result<Void> updateUserStatus(@PathVariable Integer id, @Valid @RequestBody UserStatusReq req) {
        userService.updateUserStatus(id, req);
        return Result.success();
    }

    // DELETE /api/v1/users/{id} - 管理员删除用户
    @DeleteMapping("/users/{id}")
    @RequireRole(UserType.admin)
    public Result<Void> deleteUser(@PathVariable Integer id, HttpServletRequest request) {
        Integer currentUserId = (Integer) request.getAttribute("userId");
        userService.deleteUser(id, currentUserId);
        return Result.success();
    }

    // DELETE /api/v1/users/batch - 管理员批量删除用户
    @DeleteMapping("/users/batch")
    @RequireRole(UserType.admin)
    public Result<Void> batchDeleteUsers(@Valid @RequestBody BatchDeleteReq req, HttpServletRequest request) {
        Integer currentUserId = (Integer) request.getAttribute("userId");
        userService.batchDeleteUsers(req.ids(), currentUserId);
        return Result.success();
    }
}
