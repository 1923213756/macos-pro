package com.foodmap.exception;

/**
 * 403 Forbidden - 用户权限不足
 */
public class ForbiddenException extends BaseException {
    public ForbiddenException(String message) {
        super(message);
    }
}