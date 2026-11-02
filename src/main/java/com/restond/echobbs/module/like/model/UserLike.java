package com.restond.echobbs.module.like.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户点赞实体。
 * <p>
 * 多态设计，通过 {@link TargetType} 区分点赞目标类型（帖子 / 评论），
 * {@code targetId} 指向具体的目标 ID。每个用户对同一目标只能点赞一次。
 */
@Entity
@Table(name = "user_like")
public class UserLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "target_type", nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private TargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public TargetType getTargetType() { return targetType; }
    public void setTargetType(TargetType targetType) { this.targetType = targetType; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public enum TargetType {
        POST, COMMENT
    }
}