package com.restond.echobbs.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 安全响应头过滤器：添加市面常见的 HTTP 安全头，防御点击劫持、MIME 嗅探、XSS、降级攻击等。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // 防止 MIME 类型嗅探
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");

        // 防止点击劫持（Clickjacking）：禁止被嵌入 iframe
        httpResponse.setHeader("X-Frame-Options", "DENY");

        // 启用浏览器内置 XSS 过滤器（旧版浏览器，现代浏览器依赖 CSP）
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");

        // 控制 Referrer 信息泄露
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // 限制浏览器功能权限（摄像头、麦克风、地理位置等）
        httpResponse.setHeader("Permissions-Policy",
                "camera=(), microphone=(), geolocation=(), payment=()");

        // 内容安全策略：限制资源加载来源，防御 XSS / 数据注入
        // 允许同源 + 内联样式/脚本（marked 渲染需要）+ 图片同源和 data URI + 上传图片
        httpResponse.setHeader("Content-Security-Policy",
                "default-src 'self'; " +
                "script-src 'self'; " +
                "style-src 'self' 'unsafe-inline'; " +
                "img-src 'self' data: blob:; " +
                "font-src 'self'; " +
                "connect-src 'self'; " +
                "frame-ancestors 'none'; " +
                "base-uri 'self'; " +
                "form-action 'self'");

        // HSTS：强制 HTTPS（仅对 HTTPS 请求生效，开发环境 HTTP 不受影响）
        if (httpRequest.isSecure()) {
            httpResponse.setHeader("Strict-Transport-Security",
                    "max-age=31536000; includeSubDomains");
        }

        // 对 API 响应禁止缓存（防止敏感数据被缓存）
        String uri = httpRequest.getRequestURI();
        if (uri.startsWith("/api/")) {
            httpResponse.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
            httpResponse.setHeader("Pragma", "no-cache");
        }

        chain.doFilter(request, response);
    }
}
