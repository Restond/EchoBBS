package com.restond.echobbs.module.comment.model.dto;

import com.restond.echobbs.module.comment.model.Comment;

public record CommentResponse(
        Long id,
        String content,
        Long postId,
        String postTitle,
        String boardName,
        String boardSlug,
        Long parentId,
        Long rootId,
        String authorNickname,
        String authorUsername,
        Integer likeCount,
        String status,
        String createdAt
) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getContent(),
                comment.getPost().getId(),
                comment.getPost().getTitle(),
                comment.getPost().getBoard().getName(),
                comment.getPost().getBoard().getSlug(),
                comment.getParentId(),
                comment.getRootId(),
                comment.getUser().getNickname(),
                comment.getUser().getUsername(),
                comment.getLikeCount(),
                comment.getStatus().name(),
                comment.getCreatedAt().toString()
        );
    }
}
