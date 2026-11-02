package com.restond.echobbs.module.user.controller.api;

import com.restond.echobbs.common.Result;
import com.restond.echobbs.module.comment.model.Comment;
import com.restond.echobbs.module.comment.model.dto.CommentResponse;
import com.restond.echobbs.module.comment.service.CommentService;
import com.restond.echobbs.module.post.model.Post;
import com.restond.echobbs.module.post.model.dto.PostResponse;
import com.restond.echobbs.module.post.service.PostService;
import com.restond.echobbs.module.user.model.User;
import com.restond.echobbs.module.user.model.dto.UpdateProfileRequest;
import com.restond.echobbs.module.user.model.dto.UserResponse;
import com.restond.echobbs.module.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

/**
 * 用户 API 控制器。
 * <p>
 * 提供用户信息查询、个人资料管理、头像上传、我的帖子/评论列表等接口。
 * {@code GET /api/users/me} 对游客返回 null（而非 401），用于前端探测登录态。
 */
@RestController
@RequestMapping("/api/users")
public class UserApiController {
    private final UserService userService;
    private final PostService postService;
    private final CommentService commentService;

    public UserApiController(UserService userService, CommentService commentService, PostService postService) {
        this.userService = userService;
        this.commentService = commentService;
        this.postService = postService;
    }

    @GetMapping("/me")
    public ResponseEntity<Result<UserResponse>> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        // 游客访问返回 null，前端据此判定未登录；避免 401 控制台报错
        if (userDetails == null) {
            return ResponseEntity.ok(Result.ok(null));
        }
        User user = userService.getUserByUsername(userDetails.getUsername());
        UserResponse userResponse = UserResponse.from(user);
        return ResponseEntity.ok(Result.ok(userResponse));
    }

    @GetMapping("/{username}")
    public ResponseEntity<Result<UserResponse>> getUserByUsername(@PathVariable String username) {
        User user = userService.getUserByUsername(username);
        return ResponseEntity.ok(Result.ok(UserResponse.from(user)));
    }

    @GetMapping("/{username}/stats")
    public ResponseEntity<Result<UserStatsResponse>> getUserStats(@PathVariable String username) {
        User user = userService.getUserByUsername(username);

        Page<Post> posts = postService.getUserPosts(user.getId(), 0);
        long postCount = posts.getTotalElements();
        long likeCount = posts.getContent().stream()
                .mapToLong(p -> p.getLikeCount() == null ? 0 : p.getLikeCount())
                .sum();
        long replyCount = commentService.getUserComments(user.getId(), 0).getTotalElements();

        UserStatsResponse stats = new UserStatsResponse(postCount, likeCount, replyCount);
        return ResponseEntity.ok(Result.ok(stats));
    }

    @GetMapping("/{username}/posts")
    public ResponseEntity<Result<Page<PostResponse>>> getUserPosts(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page) {
        User user = userService.getUserByUsername(username);

        Page<Post> posts = postService.getUserPosts(user.getId(), page);
        Page<PostResponse> responses = posts.map(PostResponse::from);

        return ResponseEntity.ok(Result.ok(responses));
    }

    public record UserStatsResponse(long postCount, long likeCount, long replyCount) {}

    @PutMapping("/me")
    public ResponseEntity<Result<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByUsername(userDetails.getUsername());

        User updatedUser = userService.updateProfile(user, request);
        UserResponse userResponse = UserResponse.from(updatedUser);

        return ResponseEntity.ok(Result.ok(userResponse));
    }

    @PostMapping("/me/avatar")
    public ResponseEntity<Result<UserResponse>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByUsername(userDetails.getUsername());

        try {
            User updatedUser = userService.uploadAvatar(user, file);
            UserResponse userResponse = UserResponse.from(updatedUser);
            return ResponseEntity.ok(Result.ok(userResponse));
        } catch (IOException e) {
            return ResponseEntity.status(500)
                    .body(Result.error("文件上传失败: " + e.getMessage()));
        }
    }

    @GetMapping("/me/posts")
    public ResponseEntity<Result<Page<PostResponse>>> getMyPosts(
            @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByUsername(userDetails.getUsername());

        Page<Post> posts = postService.getUserPosts(user.getId(), page);
        Page<PostResponse> responses = posts.map(PostResponse::from);

        return ResponseEntity.ok(Result.ok(responses));
    }

    @GetMapping("/me/comments")
    public ResponseEntity<Result<Page<CommentResponse>>> getMyComments(
            @RequestParam(defaultValue = "0") int page,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByUsername(userDetails.getUsername());

        Page<Comment> comments = commentService.getUserComments(user.getId(), page);
        Page<CommentResponse> responses = comments.map(CommentResponse::from);

        return ResponseEntity.ok(Result.ok(responses));
    }

    @PostMapping("/{id}/ban")
    public ResponseEntity<Result<UserResponse>> toggleBan(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = userService.getUserByUsername(userDetails.getUsername());

        User user = userService.toggleBanUser(id, currentUser);
        return ResponseEntity.ok(Result.ok(UserResponse.from(user)));
    }
}
