package com.wyb.aicodemotherme.exception;

import com.wyb.aicodemotherme.common.BaseResponse;
import com.wyb.aicodemotherme.common.ResultUtils;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Hidden //SpringDoc (Swagger) 的注解
//告诉 Swagger 不要 把这个类显示在 API 文档页面上。因为这只是处理报错的逻辑，不是提供给前端调用的接口
@RestControllerAdvice //核心注解，组合了 @ControllerAdvice 和 @ResponseBody。
//声明这是一个“切面”类，专门用来增强 Controller。它会自动捕获 Controller 抛出的异常，并将返回值直接序列化为 JSON 格式返回给前端。
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e);
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }
}
