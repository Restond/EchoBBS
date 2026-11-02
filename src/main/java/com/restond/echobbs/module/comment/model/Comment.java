package com.restond.echobbs.module.comment.model;

import com.restond.echobbs.module.post.model.Post;
import com.restond.echobbs.module.user.model.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 评论实体。
 * <p>
 * 支持楼中楼回复结构：
 * <ul>
 *   <li>{@code parentId} - 直接父评论 ID（null 表示顶层评论）</li>
 *   <li>{@code rootId} - 根评论 ID（用于将同一讨论线程的评论聚合）</li>
 * </ul>
 * 评论采用软删除（{@link CommentStatus#DELETED}），保留数据用于审计。
 */
@Entity
@Table(name = "comment")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "root_id")
    private Long rootId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "like_count", nullable = false)
    private Integer likeCount = 0;

    @Column(nullable = false, length = 16)
    @Enumerated(EnumType.STRING)
    private CommentStatus status = CommentStatus.NORMAL;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Post getPost() { return post; }
    public void setPost(Post post) { this.post = post; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public Long getRootId() { return rootId; }
    public void setRootId(Long rootId) { this.rootId = rootId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }
    public CommentStatus getStatus() { return status; }
    public void setStatus(CommentStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public enum CommentStatus {
        NORMAL, DELETED
    }
}
