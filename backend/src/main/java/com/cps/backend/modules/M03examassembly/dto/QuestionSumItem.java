package com.cps.backend.modules.M03examassembly.dto;

import com.cps.backend.modules.M02questionbank.enums.QuestionType;

// 参考 02-Data-Dictionary.md §4.3.1 — question_sum JSON items 结构
public record QuestionSumItem(
    Integer questionId,
    Integer score,
    QuestionType type
) {}
