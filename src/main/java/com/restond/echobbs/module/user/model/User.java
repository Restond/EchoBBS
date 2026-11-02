package com.restond.echobbs.module.user.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 用户实体。
 * <p>
 * 存储用户账户信息、角色和状态。表名使用 {@code "users"}（加引号）以避免 PostgreSQL 保留字冲突。
 * <ul>
 *   <li>{@link UserRole} - 角色：普通用户 / 版主 / 管理员</li>
 *   <li>{@link UserStatus} - 状态：正常 / 已封禁</li>
 * </ul>
 * 密码以 BCrypt 哈希存储（{@code passwordHash}），不保存明文。
 */
@Entity
@Table(name = "\"users\"")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String username;

    @Column(nullable = false, unique = true, length = 128)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(length = 64)
    private String nickname;

    @Column(name = "avatar_path")
    private String avatarPath;

    @Column(length = 512)
    private String bio;

    @Column(nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.USER;

    @Column(nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }

    public String getAvatarPath() { return avatarPath; }
    public void setAvatarPath(String avatarPath) { this.avatarPath = avatarPath; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public enum UserRole {
        USER, MODERATOR, ADMIN
    }

    public enum UserStatus {
        ACTIVE, BANNED
    }
}
