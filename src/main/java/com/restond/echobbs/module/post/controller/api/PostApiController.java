package com.restond.echobbs.module.post.controller.api;

import com.restond.echobbs.common.Result;
import com.restond.echobbs.module.post.model.Post;
import com.restond.echobbs.module.post.model.dto.CreatePostRequest;
import com.restond.echobbs.module.post.model.dto.PostResponse;
import com.restond.echobbs.module.post.model.dto.UpdatePostRequest;
import com.restond.echobbs.module.post.service.PostService;
import com.restond.echobbs.module.user.model.User;
import com.restond.echobbs.module.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 帖子 API 控制器。
 * <p>
 * 提供帖子的 CRUD、点赞、列表查询（支持排序和搜索）等接口。
 * <ul>
 *   <li>GET  /api/posts - 帖子列表（支持 latest/views/likes 排序和关键词搜索）</li>
 *   <li>GET  /api/posts/{id} - 帖子详情</li>
 *   <li>POST /api/posts - 发布帖子（需登录）</li>
 *   <li>PUT  /api/posts/{id} - 编辑帖子（需作者权限）</li>
 *   <li>DELETE /api/posts/{id} - 删除帖子（需作者或管理员权限）</li>
 *   <li>POST /api/posts/{id}/like - 帖子点赞切换（需登录）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/posts")
public class PostApiController {

    private final PostService postService;
    private final UserService userService;

    public PostApiController(PostService postService, UserService userService) {
        this.postService = postService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<Result<Page<PostResponse>>> getPosts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(required = false) String q) {
        Page<PostResponse> posts = postService.getPosts(page, size, sort, q)
                .map(PostResponse::from);
        return ResponseEntity.ok(Result.ok(posts));
    }

    @GetMapping("/latest")
    public ResponseEntity<Result<List<PostResponse>>> getLatestPosts(
            @RequestParam(defaultValue = "20") int limit) {
        List<PostResponse> posts = postService.getLatestPosts(limit).stream()
                .map(PostResponse::from)
                .toList();
        return ResponseEntity.ok(Result.ok(posts));
    }

    @GetMapping("/hot")
    public ResponseEntity<Result<List<PostResponse>>> getHotPosts(
            @RequestParam(defaultValue = "5") int limit) {
        List<PostResponse> posts = postService.getHotPosts(limit).stream()
                .map(PostResponse::from)
                .toList();
        return ResponseEntity.ok(Result.ok(posts));
    }

    @GetMapping("/recommended")
    public ResponseEntity<Result<List<PostResponse>>> getRecommendedPosts(
            @RequestParam(defaultValue = "5") int limit) {
        List<PostResponse> posts = postService.getHotPosts(limit).stream()
                .map(PostResponse::from)
                .toList();
        return ResponseEntity.ok(Result.ok(posts));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Result<PostResponse>> getPost(@PathVariable Long id) {
        Post post = postService.getPostById(id)
                .orElseThrow(() -> new com.restond.echobbs.common.NotFoundException("帖子不存在"));
        return ResponseEntity.ok(Result.ok(PostResponse.from(post)));
    }

    @PostMapping
    public ResponseEntity<Result<PostResponse>> createPost(
            @Valid @RequestBody CreatePostRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByUsername(userDetails.getUsername());

        Post post = postService.createPost(user, request);
        PostResponse response = PostResponse.from(post);

        return ResponseEntity.ok(Result.ok(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Result<PostResponse>> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePostRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByUsername(userDetails.getUsername());

        Post post = postService.updatePost(id, request.getTitle(), request.getContent(), user.getId());
        return ResponseEntity.ok(Result.ok(PostResponse.from(post)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Result<Void>> deletePost(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByUsername(userDetails.getUsername());

        postService.deletePost(id, user);
        return ResponseEntity.ok(Result.ok(null));
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Result<Boolean>> toggleLike(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByUsername(userDetails.getUsername());

        boolean isLiked = postService.togglePostLike(id, user);
        return ResponseEntity.ok(Result.ok(isLiked));
    }
}
