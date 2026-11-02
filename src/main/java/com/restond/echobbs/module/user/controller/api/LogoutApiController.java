package com.restond.echobbs.module.user.controller.api;

import com.restond.echobbs.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登出 API 控制器。
 * <p>
 * 销毁当前 Session 并清除 SecurityContext，完成登出。
 */
@RestController
@RequestMapping("/api")
public class LogoutApiController {
    @GetMapping("/logout")
    public Result<Void> logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        SecurityContextHolder.clearContext();

        return Result.ok();
    }
}
