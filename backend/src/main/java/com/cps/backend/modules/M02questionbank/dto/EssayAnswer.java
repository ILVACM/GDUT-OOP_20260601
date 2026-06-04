package com.cps.backend.modules.M02questionbank.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;

// 参考 02-Data-Dictionary.md §4.2.1 — Essay
@JsonTypeName("Essay")
public record EssayAnswer(
    int version,
    String reference,
    java.util.List<String> keywords,
    String scoreRule
) implements Answer {
    @Override
    public String type() { return "Essay"; }
}
