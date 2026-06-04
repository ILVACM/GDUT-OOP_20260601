package com.cps.backend.modules.M02questionbank.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;

// 参考 02-Data-Dictionary.md §4.2.1 — Judge
@JsonTypeName("Judge")
public record JudgeAnswer(
    int version,
    Boolean correct
) implements Answer {
    @Override
    public String type() { return "Judge"; }
}
