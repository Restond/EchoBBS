package com.restond.echobbs.config;

import com.restond.echobbs.module.user.model.User;
import com.restond.echobbs.module.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import java.util.Optional;

/**
 * 封禁用户拦截器。
 * <p>
 * 拦截所有请求，对已封禁（BANNED）的用户阻止其执行写操作和访问受保护页面。
 * 公开页面（首页、帖子浏览、搜索等）不拦截，允许被封禁用户浏览。
 * API 请求返回 403 JSON，页面请求重定向到封禁提示页。
 */
@Component
public class BannedUserInterceptor implements HandlerInterceptor {
    private final UserService userService;

    public BannedUserInterceptor(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        if (uri.startsWith("/api/login") || uri.startsWith("/api/register")
                || uri.startsWith("/login") || uri.startsWith("/register")
                || uri.startsWith("/css") || uri.startsWith("/js") || uri.startsWith("/images")
                || uri.equals("/") || uri.startsWith("/boards") || uri.startsWith("/posts/")
                || uri.startsWith("/users/") || uri.startsWith("/search")) {
            return true;
        }

        Optional<User> userOpt = userService.getCurrentUser();
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getStatus() == User.UserStatus.BANNED) {
                if (uri.startsWith("/api/")) {
                    response.setStatus(403);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":403,\"message\":\"账号已被封禁\",\"data\":null}");
                } else {
                    response.sendRedirect("/error/banned");
                    return false;
                }
            }
        }

        return true;
    }
}

