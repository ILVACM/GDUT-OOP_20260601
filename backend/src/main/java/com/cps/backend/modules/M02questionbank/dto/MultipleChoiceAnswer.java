package com.cps.backend.modules.M02questionbank.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;

// 参考 02-Data-Dictionary.md §4.2.1 — MultipleChoice
@JsonTypeName("MultipleChoice")
public record MultipleChoiceAnswer(
    int version,
    java.util.List<String> correctOptions,
    java.util.List<String> options
) implements Answer {
    @Override
    public String type() { return "MultipleChoice"; }
}
