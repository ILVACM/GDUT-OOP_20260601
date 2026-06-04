package com.cps.backend.common.exception;

import com.cps.backend.common.api.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常 code={} message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgument(IllegalArgumentException e) {
        return Result.error(4000, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleAny(Exception e) {
        log.error("系统异常", e);
        return Result.error(5000, "系统繁忙，请稍后重试");
    }
}
