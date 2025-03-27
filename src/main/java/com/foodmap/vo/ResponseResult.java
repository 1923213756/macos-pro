package com.foodmap.vo;

import lombok.Data;

@Data
public class ResponseResult<T> {
    private Integer code;
    private String message;
    private T data;

    public static <T> ResponseResult<T> success(String message) {
        return success(message, null);
    }

    public static <T> ResponseResult<T> success(T data) {
        return success("操作成功", data);
    }

    public static <T> ResponseResult<T> success(String message, T data) {
        ResponseResult<T> result = new ResponseResult<>();
        result.setCode(200);
        result.setMessage(message);
        result.setData(data);
        return result;
    }

    public static <T> ResponseResult<T> error(String message) {
        return error(500, message);
    }

    public static <T> ResponseResult<T> error(Integer code, String message) {
        ResponseResult<T> result = new ResponseResult<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}