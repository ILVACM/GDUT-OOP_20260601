package com.cps.backend.modules.M03examassembly.dto;

import java.util.List;

// 参考 02-Data-Dictionary.md §4.3.1 — question_sum JSON 结构
public record QuestionSum(
    int version,
    List<QuestionSumItem> items,
    int totalQuestions,
    int totalScore
) {}
