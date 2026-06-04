package com.cps.backend.modules.M04scorestatistics.dto;

import com.cps.backend.modules.M02questionbank.enums.QuestionType;
import java.util.List;

// 参考 M04-Score-Statistics.md §5.1
public record MistakeItemVO(
    Integer questionId,
    QuestionType type,
    String context,
    List<String> options,
    Object userAnswer,
    Object correctAnswer,
    Integer examId,
    String examName
) {}
