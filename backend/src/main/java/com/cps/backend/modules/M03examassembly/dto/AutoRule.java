package com.cps.backend.modules.M03examassembly.dto;

import com.cps.backend.modules.M02questionbank.enums.QuestionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

// 参考 M03-Exam-Assembly.md §5.1 — 自动组卷规则
public record AutoRule(
    @NotNull @Min(1) Integer totalQuestions,
    @NotNull @Min(1) Integer totalScore,
    List<QuestionType> typeFilter,
    Boolean usePenalty
) {}
