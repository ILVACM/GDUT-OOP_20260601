package com.cps.backend.modules.M04scorestatistics.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

// 参考 M04-Score-Statistics.md §3.1
public record ExamSubmitReq(
    @NotNull Integer examId,
    @NotEmpty List<AnswerItem> answers
) {}
