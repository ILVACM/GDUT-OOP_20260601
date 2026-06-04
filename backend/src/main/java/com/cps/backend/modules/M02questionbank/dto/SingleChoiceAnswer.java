package com.cps.backend.modules.M02questionbank.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;

// 参考 02-Data-Dictionary.md §4.2.1 — SingleChoice
@JsonTypeName("SingleChoice")
public record SingleChoiceAnswer(
    int version,
    String correctOption,
    java.util.List<String> options
) implements Answer {
    @Override
    public String type() { return "SingleChoice"; }
}
