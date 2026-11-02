package com.restond.echobbs.common;

/**
 * 资源不存在异常，对应 HTTP 404。
 * <p>
 * 在业务逻辑中抛出，由 {@link GlobalExceptionHandler} 统一捕获并转换为 404 响应。
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}