package com.restond.echobbs.module.user.controller.api;

import com.restond.echobbs.common.Result;
import com.restond.echobbs.module.user.model.dto.RegisterRequest;
import com.restond.echobbs.module.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 注册 API 控制器。
 * <p>
 * 处理用户注册请求，密码强度校验由 {@link RegisterRequest} 的注解约束。
 */
@RestController
@RequestMapping("/api")
public class RegisterApiController {
    private final UserService userService;

    public RegisterApiController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<Result<Void>> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return ResponseEntity.ok(Result.ok());
    }
}
