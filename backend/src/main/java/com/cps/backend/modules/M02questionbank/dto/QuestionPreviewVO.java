package com.cps.backend.modules.M02questionbank.dto;

import com.cps.backend.modules.M02questionbank.enums.QuestionType;

/**
 * 题目预览 VO（用于自动组卷单题获取场景）。
 * 不包含 answer 字段，防止教师预览时看到答案。
 * 参考 M03-Exam-Assembly.md — 自动组卷单题获取模式
 */
public record QuestionPreviewVO(
    Integer id,
    QuestionType type,
    String context,
    Integer img
) {}
