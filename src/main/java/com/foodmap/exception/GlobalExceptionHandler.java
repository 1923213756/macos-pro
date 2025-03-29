package com.foodmap.exception;

import com.foodmap.vo.ResponseResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)  // 返回400状态码
    public ResponseResult<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return ResponseResult.badRequest(e.getMessage());
    }

    // 可以添加其他类型的异常处理器
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)  // 返回500状态码
    public ResponseResult<Void> handleGlobalException(Exception e) {
        log.error("未处理的异常", e);
        return ResponseResult.error("服务器内部错误，请稍后再试");
    }
}