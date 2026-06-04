package com.cps.backend.modules.M04scorestatistics.dto;

import java.util.Map;

// 参考 M04-Score-Statistics.md §5.2
public record ExamStatisticsVO(
    Integer examId,
    String examName,
    Integer participantCount,
    Integer submitCount,
    Double passRate,
    Double averageScore,
    Integer maxScore,
    Integer minScore,
    Integer medianScore,
    Map<String, Integer> scoreDistribution
) {}
