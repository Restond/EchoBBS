package com.restond.echobbs.module.user.controller.api;

import com.restond.echobbs.common.Result;
import com.restond.echobbs.module.user.model.dto.LoginRequest;
import com.restond.echobbs.module.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录 API 控制器。
 * <p>
 * 处理用户登录请求，认证成功后通过 {@code changeSessionId()} 迁移会话 ID，
 * 防止会话固定攻击（Session Fixation Attack）。
 * 登录失败返回 401，错误信息不区分"用户名不存在"和"密码错误"，防止用户名枚举。
 */
@RestController
@RequestMapping("/api")
public class LoginApiController {
    private final UserService userService;

    public LoginApiController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<Result<Void>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {

        boolean success = userService.login(request, httpRequest);
        if (success) {
            httpRequest.changeSessionId();
            return ResponseEntity.ok(Result.ok());
        } else {
            return ResponseEntity.status(401).body(Result.unauthorized("用户名或密码错误"));
        }
    }
}
