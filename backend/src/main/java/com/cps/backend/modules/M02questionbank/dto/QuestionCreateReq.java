package com.cps.backend.modules.M02questionbank.dto;

import com.cps.backend.modules.M02questionbank.enums.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// 参考 M02-Question-Bank.md §6.1, §8 业务规则6
public record QuestionCreateReq(
    @NotNull QuestionType type,
    @NotBlank @Size(max = 5000) String context,
    Integer img,            // 可选，默认 0
    Object answer           // 支持字符串或对象
) {}
