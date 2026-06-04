package com.cps.backend.modules.M04scorestatistics.dto;

import com.cps.backend.modules.M02questionbank.enums.QuestionType;

// 参考 M04-Score-Statistics.md §5.1
public record DetailItemVO(
    Integer questionId,
    QuestionType type,
    String context,
    Object userAnswer,
    Object correctAnswer,
    Integer score,
    Integer maxScore,
    Boolean isCorrect
) {}
