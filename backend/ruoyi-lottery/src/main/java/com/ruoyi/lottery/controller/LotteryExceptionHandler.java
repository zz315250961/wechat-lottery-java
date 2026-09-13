package com.ruoyi.lottery.controller;

import com.ruoyi.common.core.domain.AjaxResult;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.ruoyi.lottery")
public class LotteryExceptionHandler {
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public AjaxResult business(RuntimeException error) {
        return AjaxResult.error(error.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public AjaxResult duplicate(DuplicateKeyException error) {
        return AjaxResult.error("请勿重复提交");
    }
}
