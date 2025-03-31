package com.foodmap.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "API统一响应结果")
public class ResponseResult<T> {
    // 状态码常量，便于API文档引用和代码可读性
    public static final int SUCCESS = 200;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int SERVER_ERROR = 500;

    @Schema(description = "状态码：200-成功，4xx-客户端错误，5xx-服务器错误", example = "200")
    private Integer code;

    @Schema(description = "响应消息", example = "操作成功")
    private String message;

    @Schema(description = "响应数据")
    private T data;

    // 现有的成功响应方法
    public static <T> ResponseResult<T> success(String message) {
        return success(message, null);
    }

    public static <T> ResponseResult<T> success(T data) {
        return success("操作成功", data);
    }

    public static <T> ResponseResult<T> success(String message, T data) {
        ResponseResult<T> result = new ResponseResult<>();
        result.setCode(SUCCESS);
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    // 现有的错误响应方法
    public static <T> ResponseResult<T> error(String message) {
        return error(SERVER_ERROR, message);
    }

    public static <T> ResponseResult<T> error(Integer code, String message) {
        ResponseResult<T> result = new ResponseResult<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }

    // 添加常用的错误响应便捷方法
    public static <T> ResponseResult<T> badRequest(String message) {
        return error(BAD_REQUEST, message);
    }

    public static <T> ResponseResult<T> unauthorized(String message) {
        return error(UNAUTHORIZED, message);
    }

    public static <T> ResponseResult<T> forbidden(String message) {
        return error(FORBIDDEN, message);
    }

    public static <T> ResponseResult<T> notFound(String message) {
        return error(NOT_FOUND, message);
    }
}