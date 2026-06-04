package com.cps.backend.modules.M01userauth.service;

import com.cps.backend.common.api.PageResult;
import com.cps.backend.common.exception.BusinessException;
import com.cps.backend.modules.M01userauth.dto.*;
import com.cps.backend.modules.M01userauth.entity.User;
import com.cps.backend.modules.M01userauth.enums.UserType;
import com.cps.backend.modules.M01userauth.repository.UserRepository;
import com.cps.backend.modules.M04scorestatistics.repository.ScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 参考 M01-User-Auth.md §4/§6/§7
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ScoreRepository scoreRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // 参考 M01-User-Auth.md §7 业务规则1 — 用户名唯一性
    @Transactional(rollbackFor = Exception.class)
    public UserVO register(RegisterReq req) {
        if (userRepository.existsByName(req.name())) {
            throw new BusinessException(4101, "用户名已存在");
        }
        // 参考 M01-User-Auth.md §2 权限矩阵 — 仅学生可自助注册
        if (req.type() != UserType.student) {
            throw new BusinessException(4103, "仅支持学生自助注册，教师/管理员请由管理员创建");
        }
        User user = new User();
        user.setName(req.name());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setType(req.type());
        user.setStatus(1); // 默认启用
        User saved = userRepository.save(user);
        return toVO(saved);
    }

    // 参考 M01-User-Auth.md §6 密码安全 — BCrypt 校验
    public LoginResp login(LoginReq req) {
        User user = userRepository.findByName(req.name())
            .orElseThrow(() -> new BusinessException(4101, "用户名或密码错误"));
        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new BusinessException(4101, "用户名或密码错误");
        }
        if (user.getStatus() == 0) {
            throw new BusinessException(4102, "账号已被禁用");
        }
        // Token 签发在 Controller 层完成，Service 层仅返回用户信息
        return new LoginResp(null, toVO(user));
    }

    public UserVO getCurrentUser(Integer userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(4101, "用户不存在"));
        return toVO(user);
    }

    // 参考 M01-User-Auth.md §6 — 修改密码需校验旧密码
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Integer userId, ChangePasswordReq req) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(4101, "用户不存在"));
        if (!passwordEncoder.matches(req.oldPassword(), user.getPassword())) {
            throw new BusinessException(4101, "旧密码错误");
        }
        user.setPassword(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
    }

    public PageResult<UserVO> listUsers(Integer page, Integer size, UserType type, Integer status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        if (type != null && status != null) {
            List<UserVO> filtered = userRepository.findByTypeAndStatus(type, status).stream().map(this::toVO).toList();
            int start = Math.min(page * size, filtered.size());
            int end = Math.min(start + size, filtered.size());
            return PageResult.of(new org.springframework.data.domain.PageImpl<>(filtered.subList(start, end), pageable, filtered.size()));
        } else if (type != null) {
            List<UserVO> filtered = userRepository.findByType(type).stream().map(this::toVO).toList();
            int start = Math.min(page * size, filtered.size());
            int end = Math.min(start + size, filtered.size());
            return PageResult.of(new org.springframework.data.domain.PageImpl<>(filtered.subList(start, end), pageable, filtered.size()));
        } else {
            return PageResult.of(userRepository.findAll(pageable).map(this::toVO));
        }
    }

    // 管理员创建用户（可创建任意角色）
    @Transactional(rollbackFor = Exception.class)
    public UserVO createUser(RegisterReq req) {
        if (userRepository.existsByName(req.name())) {
            throw new BusinessException(4101, "用户名已存在");
        }
        User user = new User();
        user.setName(req.name());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setType(req.type());
        user.setStatus(1);
        User saved = userRepository.save(user);
        return toVO(saved);
    }

    @Transactional(rollbackFor = Exception.class)
    public UserVO updateUser(Integer id, RegisterReq req) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new BusinessException(4101, "用户不存在"));
        // 如果修改了 name，需校验唯一性
        if (!user.getName().equals(req.name()) && userRepository.existsByName(req.name())) {
            throw new BusinessException(4101, "用户名已存在");
        }
        user.setName(req.name());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setType(req.type());
        User saved = userRepository.save(user);
        return toVO(saved);
    }

    // 参考 M01-User-Auth.md §7 业务规则2 — admin 保护
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Integer id, UserStatusReq req) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new BusinessException(4101, "用户不存在"));
        // 禁用 admin 时检查是否为最后一个
        if (req.status() == 0 && user.getType() == UserType.admin) {
            long adminCount = userRepository.findByType(UserType.admin).stream()
                .filter(u -> u.getStatus() == 1).count();
            if (adminCount <= 1) {
                throw new BusinessException(4103, "不能禁用最后一个管理员");
            }
        }
        user.setStatus(req.status());
        userRepository.save(user);
    }

    // 参考 M01-User-Auth.md §7 业务规则3/4 — 自删除保护 + 级联禁用
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Integer id, Integer currentUserId) {
        if (id.equals(currentUserId)) {
            throw new BusinessException(4103, "不能删除自己");
        }
        User user = userRepository.findById(id)
            .orElseThrow(() -> new BusinessException(4101, "用户不存在"));
        // 检查是否为最后一个 admin
        if (user.getType() == UserType.admin) {
            long adminCount = userRepository.findByType(UserType.admin).size();
            if (adminCount <= 1) {
                throw new BusinessException(4103, "不能删除最后一个管理员");
            }
        }
        // 参考 M01-User-Auth.md §7 业务规则4 — 删除用户级联：有考试记录则禁用而非删除
        if (!scoreRepository.findByUser(id).isEmpty()) {
            user.setStatus(0);
            userRepository.save(user);
            throw new BusinessException(4103, "该用户存在考试记录，已禁用而非删除");
        }
        userRepository.delete(user);
    }

    // 批量删除用户
    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteUsers(List<Integer> ids, Integer currentUserId) {
        for (Integer id : ids) {
            deleteUser(id, currentUserId);
        }
    }

    // Entity → VO 转换，参考 01-Global-Standards.md §4.3 DTO 隔离
    private UserVO toVO(User user) {
        return new UserVO(user.getId(), user.getName(), user.getType(), user.getStatus());
    }
}
