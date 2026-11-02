package com.restond.echobbs.module.post.controller.api;

import com.restond.echobbs.common.Result;
import com.restond.echobbs.module.post.model.Post;
import com.restond.echobbs.module.post.model.dto.SearchResultResponse;
import com.restond.echobbs.module.post.service.PostService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 搜索 API 控制器。
 * <p>
 * 提供帖子全文搜索接口，基于 PostgreSQL ILIKE 模糊匹配标题和内容。
 */
@RestController
public class SearchApiController {
    private final PostService postService;

    public SearchApiController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/api/search")
    public ResponseEntity<Result<Page<SearchResultResponse>>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page) {

        if (q == null || q.isBlank()) {
            return ResponseEntity.ok(Result.ok(Page.empty()));
        }

        Page<Post> posts = postService.searchPosts(q, page);
        Page<SearchResultResponse> results = posts.map(SearchResultResponse::from);

        return ResponseEntity.ok(Result.ok(results));
    }
}
