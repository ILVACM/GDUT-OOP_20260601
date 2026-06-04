package com.cps.backend.modules.M04scorestatistics.dto;

// 参考 02-Data-Dictionary.md §4.4.1 — detail JSON summary 结构
public record Summary(
    int correctCount,
    int totalCount,
    double accuracy
) {}
