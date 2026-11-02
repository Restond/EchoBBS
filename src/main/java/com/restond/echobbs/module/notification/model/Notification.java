package com.restond.echobbs.module.notification.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 通知实体。
 * <p>
 * 当用户收到回复或点赞时生成通知，通过 {@code targetUrl} 指向相关资源。
 * {@code isRead} 标记是否已读，支持批量标记已读。
 */
@Entity
@Table(name = "notification")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(nullable = false, length = 512)
    private String content;

    @Column(name = "target_url", nullable = false, length = 255)
    private String targetUrl;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public enum NotificationType { REPLY, LIKE }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getTargetUrl() { return targetUrl; }
    public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }
    public Boolean getIsRead() { return isRead; }
    public void setIsRead(Boolean isRead) { this.isRead = isRead; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
