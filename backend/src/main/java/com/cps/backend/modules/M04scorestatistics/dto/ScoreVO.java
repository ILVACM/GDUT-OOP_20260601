package com.cps.backend.modules.M04scorestatistics.dto;

import java.util.List;

// 参考 M04-Score-Statistics.md §5.1
public record ScoreVO(
    Integer id,
    Integer user,
    String userName,
    Integer exam,
    String examName,
    Integer all,
    Integer maxScore,
    Double accuracy,
    List<DetailItemVO> detail
) {}
