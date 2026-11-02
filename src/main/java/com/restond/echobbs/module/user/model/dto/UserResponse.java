package com.restond.echobbs.module.user.model.dto;

import com.restond.echobbs.module.user.model.User;
import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String email,
        String nickname,
        String avatarPath,
        String bio,
        String role,
        String status,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getNickname(),
                user.getAvatarPath(),
                user.getBio(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getCreatedAt()
        );
    }
}
