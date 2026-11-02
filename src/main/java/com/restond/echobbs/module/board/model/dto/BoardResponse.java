package com.restond.echobbs.module.board.model.dto;

import com.restond.echobbs.module.board.model.Board;

public record BoardResponse(
        Long id,
        String name,
        String slug,
        String description,
        Integer sortOrder,
        String createdAt
) {
    public static BoardResponse from(Board board) {
        return new BoardResponse(
                board.getId(),
                board.getName(),
                board.getSlug(),
                board.getDescription(),
                board.getSortOrder(),
                board.getCreatedAt().toString()
        );
    }
}
