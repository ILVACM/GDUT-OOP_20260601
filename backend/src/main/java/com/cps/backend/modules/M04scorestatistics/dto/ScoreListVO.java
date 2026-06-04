package com.cps.backend.modules.M04scorestatistics.dto;

// 参考 M04-Score-Statistics.md §6.2 — 成绩列表
public record ScoreListVO(
    Integer id,
    Integer examId,
    String examName,
    Integer totalScore,
    Integer maxScore,
    Double accuracy
) {}
