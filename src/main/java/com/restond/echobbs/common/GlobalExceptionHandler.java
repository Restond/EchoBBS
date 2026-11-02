package com.restond.echobbs.common;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器。
 * <p>
 * 统一捕获 Controller 层抛出的异常，根据请求类型（API 或页面）返回 JSON 或错误页面。
 * 同时记录安全审计日志（越权访问、注册冲突、文件过大、CSRF 失败等）。
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private boolean isApiRequest(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/");
    }


    private void logSecurityEvent(String event, HttpServletRequest request, String details) {
        String username = getCurrentUsername();
        String ip = getClientIp(request);
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String params = request.getQueryString();

        log.warn("事件={}, 用户={}, IP={}, 请求={} {}?{}, 详情={}", event, username, ip, method, uri, params, details);
    }

    /* 404 资源不存在 */
    @ExceptionHandler(NotFoundException.class)
    public Object handleNotFound(Exception ex, HttpServletRequest request) {
        if (request.getRequestURI().contains("admin") ||
                request.getRequestURI().contains("config") ||
                request.getRequestURI().contains(".env")) {
            logSecurityEvent("可疑路径访问", request, ex.getMessage());
        }

        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Result.notFound(ex.getMessage()));
        }

        ModelAndView mav = new ModelAndView("error/404");
        mav.setStatus(HttpStatus.NOT_FOUND);
        return mav;
    }

    /* 405 请求方法不支持 */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Object handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.info("请求方法不支持: {} {}, 支持的方法: {}", request.getMethod(), request.getRequestURI(), ex.getSupportedHttpMethods());

        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Result.error(405, "请求方法不支持"));
        }

        ModelAndView mav = new ModelAndView("error/404");
        mav.setStatus(HttpStatus.METHOD_NOT_ALLOWED);
        return mav;
    }

    /* 400 请求参数校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .findFirst()
                .orElse("请求参数无效");

        log.info("参数校验失败: {} {}, 错误: {}", request.getMethod(), request.getRequestURI(), message);

        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Result.badRequest(message));
        }

        ModelAndView mav = new ModelAndView("error/400");
        mav.setStatus(HttpStatus.BAD_REQUEST);
        return mav;
    }

    /* 400 业务逻辑参数异常 */
    @ExceptionHandler(IllegalArgumentException.class)
    public Object handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        if (ex.getMessage().contains("文件") || ex.getMessage().contains("上传")) {
            log.warn("文件上传失败: 用户={}, IP={}, 错误={}",
                    getCurrentUsername(), getClientIp(request), ex.getMessage());
        }

        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Result.badRequest(ex.getMessage()));
        }

        ModelAndView mav = new ModelAndView("error/400");
        mav.setStatus(HttpStatus.BAD_REQUEST);
        return mav;
    }

    /* 409 用户名/邮箱冲突 */
    @ExceptionHandler
    public Object handleConflict(ConflictException ex, HttpServletRequest request) {
        logSecurityEvent("注册冲突", request, ex.getMessage());

        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Result.conflict(ex.getMessage()));
        }

        ModelAndView mav = new ModelAndView("error/400");
        mav.setStatus(HttpStatus.CONFLICT);
        return mav;
    }

    /* 413 文件过大 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Object handleMaxUploadSize(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        logSecurityEvent("文件过大", request, "超过 5MB 限制");

        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Result.error(413, "文件大小超过限制（最大 5MB）"));
        }

        ModelAndView mav = new ModelAndView("error/400");
        mav.setStatus(HttpStatus.REQUEST_HEADER_FIELDS_TOO_LARGE);
        return mav;
    }

    /* 403 无权限 */
    @ExceptionHandler(ForbiddenException.class)
    public Object handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        logSecurityEvent("越权访问", request, ex.getMessage());

        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Result.forbidden(ex.getMessage()));
        }
        ModelAndView mav = new ModelAndView("error/403");
        mav.setStatus(HttpStatus.FORBIDDEN);
        return mav;
    }

    /* 404 静态资源不存在 */
    @ExceptionHandler(NoResourceFoundException.class)
    public Object handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Result.notFound("资源不存在"));
        }
        ModelAndView mav = new ModelAndView("error/404");
        mav.setStatus(HttpStatus.NOT_FOUND);
        return mav;
    }

    /* 500 其他未捕获异常 */
    @ExceptionHandler(Exception.class)
    public Object handleException(Exception ex, HttpServletRequest request) {
        String username = getCurrentUsername();
        String ip = getClientIp(request);
        String uri = request.getRequestURI();
        String method = request.getMethod();

        log.error("[严重] 用户={}, IP={}, 请求={} {}, 异常类型={}, 消息={}",
                username, ip, method, uri, ex.getClass().getSimpleName(), ex.getMessage(), ex);


        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Result.error("服务器内部错误，请稍后重试"));
        }

        ModelAndView mav = new ModelAndView("error/500");
        mav.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        return mav;
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "匿名用户";
        }
        return auth.getName();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Object handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        logSecurityEvent("CSRF验证失败或权限不足", request, ex.getMessage());

        if (isApiRequest(request)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Result.forbidden("CSRF Token 验证失败或权限不足，请刷新页面后重试"));
        }

        ModelAndView mav = new ModelAndView("error/403");
        mav.setStatus(HttpStatus.FORBIDDEN);
        return mav;
    }
}
