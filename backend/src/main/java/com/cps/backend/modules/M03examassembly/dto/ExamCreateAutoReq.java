package com.cps.backend.modules.M03examassembly.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;

// 参考 M03-Exam-Assembly.md §5.1 — 自动组卷请求
public record ExamCreateAutoReq(
    @NotBlank String exam,
    @NotNull String starttime,
    @NotNull String endtime,
    @NotNull @Valid AutoRule autoRule
) {}
