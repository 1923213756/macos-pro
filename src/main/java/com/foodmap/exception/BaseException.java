package com.foodmap.exception;

/**
 * 应用基础异常类，所有自定义异常的父类
 */
public abstract class BaseException extends RuntimeException {
    public BaseException(String message) {
        super(message);
    }

    public BaseException(String message, Throwable cause) {
        super(message, cause);
    }
}