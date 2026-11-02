package com.restond.echobbs.module.post.model.dto;

import com.restond.echobbs.module.post.model.Post;
import java.time.LocalDateTime;

public record PostListResponse(
        Long id,
        String title,
        Long boardId,
        String boardName,
        String boardSlug,
        Long userId,
        String username,
        String nickname,
        String avatarPath,
        Integer viewCount,
        Integer likeCount,
        Boolean isPinned,
        Boolean isLocked,
        LocalDateTime createdAt,
        LocalDateTime lastRepliedAt
) {
    public static PostListResponse from(Post post) {
        return new PostListResponse(
                post.getId(),
                post.getTitle(),
                post.getBoard().getId(),
                post.getBoard().getName(),
                post.getBoard().getSlug(),
                post.getUser().getId(),
                post.getUser().getUsername(),
                post.getUser().getNickname(),
                post.getUser().getAvatarPath(),
                post.getViewCount(),
                post.getLikeCount(),
                post.getIsPinned(),
                post.getIsLocked(),
                post.getCreatedAt(),
                post.getLastRepliedAt()
        );
    }
}
