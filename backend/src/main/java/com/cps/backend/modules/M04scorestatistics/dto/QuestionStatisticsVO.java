package com.cps.backend.modules.M04scorestatistics.dto;

import com.cps.backend.modules.M02questionbank.enums.QuestionType;

// 参考 M04-Score-Statistics.md §5.2
public record QuestionStatisticsVO(
    Integer questionId,
    QuestionType type,
    Integer use,
    Integer correct,
    Double accuracyRate
) {}
