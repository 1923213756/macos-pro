package com.foodmap.exception;

/**
 * 409 Conflict - 请求冲突，例如资源已存在
 */
public class ConflictException extends BaseException {
    public ConflictException(String message) {
        super(message);
    }
}