package com.cps.backend.modules.M02questionbank.dto;

import jakarta.validation.constraints.NotBlank;

// 参考 M02-Question-Bank.md §6.1 — 更新时 type 不可变
public record QuestionUpdateReq(
    @NotBlank String context,
    Integer img,
    @NotBlank String answer  // JSON 字符串
) {}
