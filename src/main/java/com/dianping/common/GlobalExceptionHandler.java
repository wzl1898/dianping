package com.dianping.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusiness(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.error(e.getErrorCode());
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handleUnknown(Exception e) {
        log.error("未知异常", e);
        return Result.error(ErrorCode.UNKNOWN_ERROR);
    }
}
