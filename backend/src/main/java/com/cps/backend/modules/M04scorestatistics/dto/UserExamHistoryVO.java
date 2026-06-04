package com.cps.backend.modules.M04scorestatistics.dto;

// 参考 M04-Score-Statistics.md §5.2
public record UserExamHistoryVO(
    Integer examId,
    String examName,
    Integer all,
    Integer maxScore
) {}
