package com.cps.backend.modules.M03examassembly.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// 参考 M03-Exam-Assembly.md §5.1
public record ExamQuestionItemReq(
    @NotNull Integer questionId,
    @NotNull @Min(1) Integer score
) {}
