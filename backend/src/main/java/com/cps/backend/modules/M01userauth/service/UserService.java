package com.cps.backend.modules.M01userauth.service;

import com.cps.backend.common.exception.BusinessException;
import com.cps.backend.modules.M01userauth.dto.*;
import com.cps.backend.modules.M01userauth.entity.User;
import com.cps.backend.modules.M01userauth.enums.UserType;
import com.cps.backend.modules.M01userauth.repository.UserRepository;
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
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // 参考 M01-User-Auth.md §7 业务规则1 — 用户名唯一性
    @Transactional(rollbackFor = Exception.class)
    public UserVO register(RegisterReq req) {
        if (userRepository.existsByName(req.name())) {
            throw new BusinessException(4101, "用户名已存在");
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

    public UserVO findById(Integer id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new BusinessException(4101, "用户不存在"));
        return toVO(user);
    }

    public List<UserVO> findByTypeAndStatus(UserType type, Integer status) {
        if (type != null && status != null) {
            return userRepository.findByTypeAndStatus(type, status).stream().map(this::toVO).toList();
        } else if (type != null) {
            return userRepository.findByType(type).stream().map(this::toVO).toList();
        } else {
            return userRepository.findAll().stream().map(this::toVO).toList();
        }
    }

    public Page<UserVO> findPage(UserType type, Integer status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        // 简化实现：先查全量再过滤分页（题库规模小，可接受）
        // 生产环境应使用 JPA Specification 动态查询
        if (type != null && status != null) {
            List<UserVO> filtered = userRepository.findByTypeAndStatus(type, status).stream().map(this::toVO).toList();
            int start = Math.min(page * size, filtered.size());
            int end = Math.min(start + size, filtered.size());
            return new org.springframework.data.domain.PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
        } else if (type != null) {
            List<UserVO> filtered = userRepository.findByType(type).stream().map(this::toVO).toList();
            int start = Math.min(page * size, filtered.size());
            int end = Math.min(start + size, filtered.size());
            return new org.springframework.data.domain.PageImpl<>(filtered.subList(start, end), pageable, filtered.size());
        } else {
            return userRepository.findAll(pageable).map(this::toVO);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public UserVO update(Integer id, RegisterReq req) {
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

    // 参考 M01-User-Auth.md §7 业务规则2 — admin 保护
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Integer id, UserStatusReq req) {
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
    public void delete(Integer id, Integer currentUserId) {
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
        // TODO: 检查关联未结束考试/未批改答卷，如有则禁用而非删除
        // 当前简化实现：直接删除（ScoreRepository 跨模块依赖待 M04 完善后补充）
        userRepository.delete(user);
    }

    // Entity → VO 转换，参考 01-Global-Standards.md §4.3 DTO 隔离
    private UserVO toVO(User user) {
        return new UserVO(user.getId(), user.getName(), user.getType(), user.getStatus());
    }
}
