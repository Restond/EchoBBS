package com.restond.echobbs.module.comment.controller.api;

import com.restond.echobbs.common.Result;
import com.restond.echobbs.module.comment.model.Comment;
import com.restond.echobbs.module.comment.model.dto.CommentResponse;
import com.restond.echobbs.module.comment.model.dto.CreateCommentRequest;
import com.restond.echobbs.module.comment.service.CommentService;
import com.restond.echobbs.module.user.model.User;
import com.restond.echobbs.module.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 评论 API 控制器。
 * <p>
 * 提供评论的发表、楼中楼回复、删除、点赞等接口。
 * <ul>
 *   <li>GET    /api/posts/{postId}/comments - 获取帖子评论列表（扁平结构，前端组装层级）</li>
 *   <li>POST   /api/posts/{postId}/comments - 发表顶层评论（需登录）</li>
 *   <li>POST   /api/comments/{commentId}/reply - 回复评论（楼中楼，需登录）</li>
 *   <li>DELETE /api/comments/{commentId} - 删除评论（需作者或管理员权限，软删除）</li>
 *   <li>POST   /api/comments/{commentId}/like - 评论点赞切换（需登录）</li>
 * </ul>
 */
@RestController
public class CommentApiController {
    private final CommentService commentService;
    private final UserService userService;

    public CommentApiController(CommentService commentService, UserService userService) {
        this.commentService = commentService;
        this.userService = userService;
    }

    @GetMapping("/api/posts/{postId}/comments")
    public ResponseEntity<Result<List<CommentResponse>>> getComments(@PathVariable Long postId) {
        List<CommentResponse> comments = commentService.getCommentsByPostId(postId).stream()
                .map(CommentResponse::from)
                .toList();
        return ResponseEntity.ok(Result.ok(comments));
    }

    @PostMapping("/api/posts/{postId}/comments")
    public ResponseEntity<Result<CommentResponse>> addComment(
            @PathVariable Long postId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByUsername(userDetails.getUsername());

        Comment comment = commentService.addComment(postId, request.content(), user);
        return ResponseEntity.ok(Result.ok(CommentResponse.from(comment)));
    }

    @PostMapping("/api/comments/{commentId}/reply")
    public ResponseEntity<Result<CommentResponse>> replyComment(
            @PathVariable Long commentId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByUsername(userDetails.getUsername());

        Comment comment = commentService.replyComment(commentId, request.content(), user);
        return ResponseEntity.ok(Result.ok(CommentResponse.from(comment)));
    }

    @DeleteMapping("/api/comments/{commentId}")
    public ResponseEntity<Result<Void>> deleteComment(@PathVariable Long commentId, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByUsername(userDetails.getUsername());

        commentService.deleteComment(commentId, user);
        return ResponseEntity.ok(Result.ok(null));
    }

    @PostMapping("/api/comments/{commentId}/like")
    public ResponseEntity<Result<Boolean>> toggleCommentLike(@PathVariable Long commentId, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByUsername(userDetails.getUsername());

        boolean isLiked = commentService.toggleCommentLike(commentId, user);
        return ResponseEntity.ok(Result.ok(isLiked));
    }
}
