package com.restond.echobbs.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API 限流拦截器：基于 IP + URI 维度。
 * - 登录/注册：5 次/分钟（防爆破）
 * - 写操作（POST/PUT/DELETE）：30 次/分钟（防滥用发帖/评论/上传）
 * - 读操作：宽松，不限流
 */
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    private final Map<String, Bucket> authCache = new ConcurrentHashMap<>();
    private final Map<String, Bucket> writeCache = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String ip = getClientIp(request);

        // 1. 登录/注册：5 次/分钟
        if (uri.startsWith("/api/login") || uri.startsWith("/api/register")) {
            Bucket bucket = authCache.computeIfAbsent("auth:" + ip, k -> createAuthBucket());
            if (!bucket.tryConsume(1)) {
                return tooManyRequests(response);
            }
        }

        // 2. 写操作（排除登录注册）：30 次/分钟
        if (("POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method))
                && uri.startsWith("/api/")
                && !uri.startsWith("/api/login") && !uri.startsWith("/api/register")
                && !uri.startsWith("/api/logout")) {
            Bucket bucket = writeCache.computeIfAbsent("write:" + ip, k -> createWriteBucket());
            if (!bucket.tryConsume(1)) {
                return tooManyRequests(response);
            }
        }

        return true;
    }

    private boolean tooManyRequests(HttpServletResponse response) throws java.io.IOException {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\",\"data\":null}");
        return false;
    }

    private Bucket createAuthBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(5)
                .refillIntervally(5, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createWriteBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(30)
                .refillIntervally(30, Duration.ofMinutes(1))
                .build();
        return Bucket.builder().addLimit(limit).build();
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
}
