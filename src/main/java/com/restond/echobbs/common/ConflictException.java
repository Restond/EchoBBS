package com.restond.echobbs.common;

/**
 * 资源冲突异常，对应 HTTP 409。
 * <p>
 * 主要用于注册时用户名/邮箱已存在等场景。
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
