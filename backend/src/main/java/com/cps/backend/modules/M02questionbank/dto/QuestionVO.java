package com.cps.backend.modules.M02questionbank.dto;

import com.cps.backend.modules.M02questionbank.enums.QuestionType;

// 参考 M02-Question-Bank.md §6.2
public record QuestionVO(
    Integer id,
    QuestionType type,
    String context,
    Integer img,
    String answer,    // JSON 字符串（前端按需解析）
    Integer use,
    Integer correct,
    Double accuracy   // correct / use，use=0 时为 null
) {}
