package com.cps.backend.modules.M02questionbank.dto;

import com.fasterxml.jackson.annotation.JsonTypeName;

// 参考 02-Data-Dictionary.md §4.2.1 — Fill
@JsonTypeName("Fill")
public record FillAnswer(
    int version,
    java.util.List<String> blanks
) implements Answer {
    @Override
    public String type() { return "Fill"; }
}
