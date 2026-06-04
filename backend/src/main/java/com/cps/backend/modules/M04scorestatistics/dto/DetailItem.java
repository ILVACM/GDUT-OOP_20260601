package com.cps.backend.modules.M04scorestatistics.dto;

import com.cps.backend.modules.M02questionbank.enums.QuestionType;

// 参考 02-Data-Dictionary.md §4.4.1 — detail JSON items 结构
public record DetailItem(
    Integer questionId,
    Object userAnswer,      // 多态：单选String, 多选List, 填空List, 简答String
    Object correctAnswer,   // 标准答案
    Integer score,
    Boolean isCorrect       // null 表示待评卷（Essay）
) {}
