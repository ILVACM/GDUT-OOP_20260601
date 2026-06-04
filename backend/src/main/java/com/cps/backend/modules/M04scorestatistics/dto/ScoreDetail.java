package com.cps.backend.modules.M04scorestatistics.dto;

import java.util.List;

// 参考 02-Data-Dictionary.md §4.4.1 — detail JSON 结构
public record ScoreDetail(
    int version,
    List<DetailItem> items,
    Summary summary
) {}
