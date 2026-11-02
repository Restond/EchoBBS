package com.restond.echobbs.common;

/**
 * 权限不足异常，对应 HTTP 403。
 * <p>
 * 当用户尝试操作无权限的资源时抛出，由 {@link GlobalExceptionHandler} 统一捕获。
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
