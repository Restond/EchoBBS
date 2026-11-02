package com.restond.echobbs.module.notification.model.dto;

import com.restond.echobbs.module.notification.model.Notification;
import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        String type,
        String content,
        String targetUrl,
        Boolean isRead,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType().name(),
                n.getContent(),
                n.getTargetUrl(),
                n.getIsRead(),
                n.getCreatedAt()
        );
    }
}
