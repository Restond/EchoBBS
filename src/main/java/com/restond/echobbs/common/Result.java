package com.restond.echobbs.common;

/**
 * 统一 API 响应封装。
 * <p>
 * 所有 REST API 返回 {@code Result<T>} 格式，包含状态码、消息和数据体，
 * 便于前端统一处理响应结构。
 *
 * @param code    HTTP 状态码（200/400/401/403/404/409/500）
 * @param message 响应消息
 * @param data    响应数据，失败时为 null
 * @param <T>     数据类型
 */
public record Result<T>(int code, String message, T data) {

    // 成功
    public static <T> Result<T> ok(T data) {
        return new Result<>(200, "ok", data);
    }

    public static <T> Result<T> ok() {
        return new Result<>(200, "ok", null);
    }

    // 客户端错误
    public static <T> Result<T> badRequest(String message) {
        return new Result<>(400, message, null);
    }

    public static <T> Result<T> unauthorized(String message) {
        return new Result<>(401, message, null);
    }

    public static <T> Result<T> forbidden(String message) {
        return new Result<>(403, message, null);
    }

    public static <T> Result<T> notFound(String message) {
        return new Result<>(404, message, null);
    }

    public static <T> Result<T> conflict(String message) {
        return new Result<>(409, message, null);
    }

    // 服务端错误
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }
}
