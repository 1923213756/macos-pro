package com.foodmap.exception;

import com.foodmap.common.response.ResponseResult;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.HttpStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseResult<Void> handleBaseException(BaseException e) {
        log.warn("应用异常: {}", e.getMessage());

        // 根据异常类型设置不同的状态码
        if (e instanceof BadRequestException) {
            return ResponseResult.badRequest(e.getMessage());
        } else if (e instanceof UnauthorizedException) {
            return ResponseResult.unauthorized(e.getMessage());
        } else if (e instanceof ForbiddenException) {
            return ResponseResult.forbidden(e.getMessage());
        } else if (e instanceof NotFoundException) {
            return ResponseResult.notFound(e.getMessage());
        } else if (e instanceof ConflictException) {
            return ResponseResult.error(409, e.getMessage());
        } else {
            return ResponseResult.error(e.getMessage());
        }
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseResult<Void> handleValidationException(Exception e) {
        String message;
        if (e instanceof MethodArgumentNotValidException) {
            message = ((MethodArgumentNotValidException) e).getBindingResult().getFieldErrors()
                    .stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining(", "));
        } else {
            message = ((BindException) e).getBindingResult().getFieldErrors()
                    .stream()
                    .map(FieldError::getDefaultMessage)
                    .collect(Collectors.joining(", "));
        }

        log.warn("参数校验失败: {}", message);
        return ResponseResult.badRequest(message);
    }

    // 可以添加其他类型的异常处理器
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)  // 返回500状态码
    public ResponseResult<Void> handleGlobalException(Exception e) {
        log.error("未处理的异常", e);
        return ResponseResult.error("服务器内部错误，请稍后再试");
    }
}