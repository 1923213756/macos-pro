package com.foodmap.exception;

/**
 * 404 Not Found - 请求的资源不存在
 */
public class NotFoundException extends BaseException {
    public NotFoundException(String message) {
        super(message);
    }
}