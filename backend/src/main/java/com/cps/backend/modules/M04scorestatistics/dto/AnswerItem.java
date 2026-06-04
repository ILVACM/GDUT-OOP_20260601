package com.cps.backend.modules.M04scorestatistics.dto;

import jakarta.validation.constraints.NotNull;

// 参考 M04-Score-Statistics.md §3.1
public record AnswerItem(
    @NotNull Integer questionId,
    Object userAnswer   // 多态：单选String, 多选List<String>, 判断Boolean, 填空List<String>, 简答String
) {}
