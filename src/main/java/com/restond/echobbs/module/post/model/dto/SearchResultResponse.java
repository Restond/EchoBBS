package com.restond.echobbs.module.post.model.dto;

import com.restond.echobbs.module.post.model.Post;

public record SearchResultResponse(
        Long id,
        String title,
        String excerpt,
        String boardName,
        String boardSlug,
        String authorNickname,
        String authorUsername,
        Integer viewCount,
        Integer likeCount,
        String createdAt
) {
    public static SearchResultResponse from(Post post) {
        String excerpt = post.getContent().length() > 200
                ? post.getContent().substring(0, 200) + "..."
                : post.getContent();

        return new SearchResultResponse(
                post.getId(),
                post.getTitle(),
                excerpt,
                post.getBoard().getName(),
                post.getBoard().getSlug(),
                post.getUser().getNickname(),
                post.getUser().getUsername(),
                post.getViewCount(),
                post.getLikeCount(),
                post.getCreatedAt().toString()
        );
    }
}
