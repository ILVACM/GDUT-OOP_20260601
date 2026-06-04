package com.cps.backend.modules.M02questionbank.dto;

import com.cps.backend.modules.M02questionbank.enums.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// 参考 M02-Question-Bank.md §6.1
public record QuestionCreateReq(
    @NotNull QuestionType type,
    @NotBlank String context,
    Integer img,            // 可选，默认 0
    @NotNull String answer  // JSON 字符串
) {}
