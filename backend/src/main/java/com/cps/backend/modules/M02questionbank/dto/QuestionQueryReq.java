package com.cps.backend.modules.M02questionbank.dto;

import com.cps.backend.modules.M02questionbank.enums.QuestionType;

// 参考 M02-Question-Bank.md §6.1
public record QuestionQueryReq(
    QuestionType type,
    String keyword,
    Integer page,
    Integer size
) {
    public QuestionQueryReq {
        if (page == null) page = 0;
        if (size == null) size = 20;
    }
}
