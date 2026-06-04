package com.cps.backend.modules.M01userauth.dto;

import com.cps.backend.modules.M01userauth.enums.UserType;

// 参考 M01-User-Auth.md §3.2 — password 字段严禁出现在 VO 中
public record UserVO(
    Integer id,
    String name,
    UserType type,
    Integer status
) {}
