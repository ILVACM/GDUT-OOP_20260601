package com.cps.backend.modules.M03examassembly.dto;

import com.cps.backend.modules.M02questionbank.enums.QuestionType;

// 参考 M03-Exam-Assembly.md §5.2 — 学生视角题目项（不含答案）
public record ExamQuestionForStudentVO(
    Integer questionId,
    QuestionType type,
    String context,
    Integer img,
    String imageUrl,      // 图片 URL（img=1 时返回，否则为 null）
    Object options,  // 从 question.answer.options 提取（仅单选/多选）
    Integer score
) {}
