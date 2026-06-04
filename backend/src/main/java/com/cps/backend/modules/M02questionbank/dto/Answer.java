package com.cps.backend.modules.M02questionbank.dto;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

// 参考 02-Data-Dictionary.md §4.2.1 — answer JSON 按 type 自适应
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = SingleChoiceAnswer.class, name = "SingleChoice"),
    @JsonSubTypes.Type(value = MultipleChoiceAnswer.class, name = "MultipleChoice"),
    @JsonSubTypes.Type(value = JudgeAnswer.class, name = "Judge"),
    @JsonSubTypes.Type(value = FillAnswer.class, name = "Fill"),
    @JsonSubTypes.Type(value = EssayAnswer.class, name = "Essay")
})
public sealed interface Answer permits
    SingleChoiceAnswer, MultipleChoiceAnswer, JudgeAnswer, FillAnswer, EssayAnswer {
    int version();
    String type();
}
