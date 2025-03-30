package com.foodmap.exception;

/**
 * 400 Bad Request - 客户端请求参数错误或不合法
 */
public class BadRequestException extends BaseException {
    public BadRequestException(String message) {
        super(message);
    }
}