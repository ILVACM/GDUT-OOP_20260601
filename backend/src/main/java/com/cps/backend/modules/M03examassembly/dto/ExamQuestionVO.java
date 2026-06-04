package com.cps.backend.modules.M03examassembly.dto;

import com.cps.backend.modules.M02questionbank.enums.QuestionType;

// 参考 M03-Exam-Assembly.md §5.2 — 教师视角题目项
public record ExamQuestionVO(
    Integer questionId,
    QuestionType type,
    Integer score
) {}
