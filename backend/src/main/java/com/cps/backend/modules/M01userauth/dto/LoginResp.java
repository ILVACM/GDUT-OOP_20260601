package com.cps.backend.modules.M01userauth.dto;

// 参考 M01-User-Auth.md §3.2
public record LoginResp(
    String token,
    UserVO user
) {}
