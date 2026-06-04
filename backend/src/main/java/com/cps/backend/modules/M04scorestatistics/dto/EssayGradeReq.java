package com.cps.backend.modules.M04scorestatistics.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

// 参考 M04-Score-Statistics.md §4.1
public record EssayGradeReq(
    @NotNull Integer questionId,
    @NotNull @Min(0) Integer score,
    String comment
) {}
