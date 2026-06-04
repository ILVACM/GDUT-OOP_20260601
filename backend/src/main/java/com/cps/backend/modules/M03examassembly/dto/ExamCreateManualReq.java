package com.cps.backend.modules.M03examassembly.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

// 参考 M03-Exam-Assembly.md §5.1 — 手动组卷请求
public record ExamCreateManualReq(
    @NotBlank String exam,
    @NotNull String starttime,
    @NotNull String endtime,
    @NotEmpty List<ExamQuestionItemReq> items
) {}
