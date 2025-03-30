package com.foodmap.exception;

/**
 * 401 Unauthorized - 用户未认证或认证已过期
 */
public class UnauthorizedException extends BaseException {
    public UnauthorizedException(String message) {
        super(message);
    }
}