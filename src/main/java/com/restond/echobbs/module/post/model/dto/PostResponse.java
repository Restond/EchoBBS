package com.restond.echobbs.module.post.model.dto;

import com.restond.echobbs.module.post.model.Post;
import java.time.LocalDateTime;

public record PostResponse(
        Long id,
        String title,
        String content,
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
        LocalDateTime lastRepliedAt,
        LocalDateTime updatedAt
) {
    public static PostResponse from(Post post) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
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
                post.getLastRepliedAt(),
                post.getUpdatedAt()
        );
    }
}