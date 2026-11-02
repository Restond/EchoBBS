package com.restond.echobbs.module.user.controller.api;

import com.restond.echobbs.common.Result;
import com.restond.echobbs.module.user.model.User;
import com.restond.echobbs.module.user.model.dto.UserResponse;
import com.restond.echobbs.module.user.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理 API 控制器（管理员专用）。
 * <p>
 * 提供用户列表查询接口，需要 ADMIN 或 MODERATOR 角色。
 * 用户封禁/解封操作在 {@link UserApiController#toggleBan} 中实现。
 */
@RestController
@RequestMapping("/api/admin/users")
public class UserAdminApiController {
    private final UserService userService;

    public UserAdminApiController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<Result<Page<UserResponse>>> listAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<User> users = userService.getAllUsers(page, size);
        Page<UserResponse> responses = users.map(UserResponse::from);
        return ResponseEntity.ok(Result.ok(responses));
    }
}
