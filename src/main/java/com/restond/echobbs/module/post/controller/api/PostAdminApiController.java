package com.restond.echobbs.module.post.controller.api;

import com.restond.echobbs.common.Result;
import com.restond.echobbs.module.post.model.Post;
import com.restond.echobbs.module.post.model.dto.PostResponse;
import com.restond.echobbs.module.post.service.PostService;
import com.restond.echobbs.module.user.model.User;
import com.restond.echobbs.module.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 帖子管理 API 控制器（管理员专用）。
 * <p>
 * 提供帖子置顶和锁定管理接口，需要 ADMIN 或 MODERATOR 角色。
 * <ul>
 *   <li>POST /api/admin/posts/{id}/pin - 帖子置顶切换</li>
 *   <li>POST /api/admin/posts/{id}/lock - 帖子锁定切换</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin/posts")
public class PostAdminApiController {
    private final PostService postService;
    private final UserService userService;

    public PostAdminApiController(PostService postService, UserService userService) {
        this.postService = postService;
        this.userService = userService;
    }

    @PostMapping("/{id}/pin")
    public ResponseEntity<Result<PostResponse>> togglePin(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByUsername(userDetails.getUsername());

        Post post = postService.togglePin(id, user);
        return ResponseEntity.ok(Result.ok(PostResponse.from(post)));
    }

    @PostMapping("/{id}/lock")
    public ResponseEntity<Result<PostResponse>> toggleLock(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByUsername(userDetails.getUsername());

        Post post = postService.toggleLock(id, user);
        return ResponseEntity.ok(Result.ok(PostResponse.from(post)));
    }
}
