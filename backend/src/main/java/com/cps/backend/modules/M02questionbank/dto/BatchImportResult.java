package com.cps.backend.modules.M02questionbank.dto;

import java.util.List;

// 参考 M02-Question-Bank.md §8 业务规则4 — 批量导入结果
public record BatchImportResult(
    int successCount,
    int failCount,
    List<ImportError> errors
) {
    public record ImportError(int index, String message) {}
}
