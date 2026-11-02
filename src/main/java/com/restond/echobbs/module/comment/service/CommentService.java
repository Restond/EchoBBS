package com.restond.echobbs.module.comment.service;

import com.restond.echobbs.common.ForbiddenException;
import com.restond.echobbs.common.NotFoundException;
import com.restond.echobbs.module.comment.model.Comment;
import com.restond.echobbs.module.comment.repository.CommentRepository;
import com.restond.echobbs.module.like.model.UserLike;
import com.restond.echobbs.module.like.repository.UserLikeRepository;
import com.restond.echobbs.module.notification.model.Notification;
import com.restond.echobbs.module.notification.service.NotificationService;
import com.restond.echobbs.module.post.model.Post;
import com.restond.echobbs.module.post.repository.PostRepository;
import com.restond.echobbs.module.user.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 评论业务服务。
 * <p>
 * 处理评论的发表、楼中楼回复、删除（软删除）、点赞等业务逻辑。
 * 回复评论时自动维护 {@code parentId} 和 {@code rootId} 层级关系，
 * 并通过 {@link NotificationService} 向被回复者发送通知。
 */
@Service
public class CommentService {
    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserLikeRepository userLikeRepository;
    private final NotificationService notificationService;

    public CommentService(CommentRepository commentRepository, PostRepository postRepository,
                          UserLikeRepository userLikeRepository, NotificationService notificationService) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userLikeRepository = userLikeRepository;
        this.notificationService = notificationService;
    }

    public List<Comment> getCommentsByPostId(Long postId) {
        return commentRepository.findByPostIdFlattened(postId);
    }

    @Transactional
    public Comment addComment(Long postId, String content, User user) {
        Post post = postRepository.findByIdAndStatusWithDetails(postId, Post.PostStatus.NORMAL)
                .orElseThrow(() -> new NotFoundException("帖子不存在"));

        if (post.getIsLocked()) {
            throw new ForbiddenException("帖子已锁定，无法评论");
        }

        Comment comment = new Comment();
        comment.setPost(post);
        comment.setUser(user);
        comment.setContent(content);

        Comment saved = commentRepository.save(comment);

        post.setLastRepliedAt(LocalDateTime.now());
        postRepository.save(post);

        Long postAuthorId = post.getUser().getId();
        if (!postAuthorId.equals(user.getId())) {
            notificationService.send(postAuthorId, Notification.NotificationType.REPLY,
                    user.getNickname() + " 回复了你的帖子《" + post.getTitle() + "》",
                    "/posts/" + post.getId());
        }

        return saved;
    }

    /**
     * 回复评论（楼中楼）。
     * <p>
     * 层级关系维护：若父评论本身是顶层评论，则新评论的 rootId 设为父评论 ID；
     * 若父评论是子回复，则继承其 rootId，保持同一讨论线程的聚合。
     * 回复后向被回复者发送通知（不通知自己）。
     *
     * @param parentCommentId 被回复的父评论 ID
     * @param content         回复内容
     * @param user            回复者
     * @return 已保存的评论实体
     */
    @Transactional
    public Comment replyComment(Long parentCommentId, String content, User user) {
        Comment parent = commentRepository.findByIdWithDetails(parentCommentId)
                .orElseThrow(() -> new NotFoundException("评论不存在"));

        Post post = parent.getPost();
        if (post.getIsLocked()) {
            throw new ForbiddenException("帖子已锁定，无法评论");
        }

        Long rootId = parent.getRootId() == null ? parent.getId() : parent.getRootId();

        Comment comment = new Comment();
        comment.setPost(post);
        comment.setUser(user);
        comment.setContent(content);
        comment.setParentId(parent.getId());
        comment.setRootId(rootId);

        Comment saved = commentRepository.save(comment);

        post.setLastRepliedAt(LocalDateTime.now());
        postRepository.save(post);

        Long parentAuthorId = parent.getUser().getId();
        if (!parentAuthorId.equals(user.getId())) {
            notificationService.send(parentAuthorId, Notification.NotificationType.REPLY,
                    user.getNickname() + " 回复了你在《" + post.getTitle() + "》中的评论",
                    "/posts/" + post.getId());
        }

        return saved;
    }

    /**
     * 删除评论（软删除）。
     * <p>
     * 权限规则：评论作者或管理员（ADMIN/MODERATOR）可删除。
     * 删除操作仅将状态置为 DELETED，保留数据用于审计。
     *
     * @param commentId   评论 ID
     * @param currentUser 当前操作用户
     */
    @Transactional
    public void deleteComment(Long commentId, User currentUser) {
        Comment comment = commentRepository.findByIdWithDetails(commentId)
                .orElseThrow(() -> new NotFoundException("评论不存在"));

        boolean isOwner = comment.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == User.UserRole.ADMIN
                || currentUser.getRole() == User.UserRole.MODERATOR;

        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("无权删除此评论");
        }

        comment.setStatus(Comment.CommentStatus.DELETED);
        commentRepository.save(comment);
    }

    /**
     * 切换评论点赞状态。
     * <p>
     * 已点赞则取消（删除 UserLike 记录，点赞数 -1），未点赞则新增（点赞数 +1）。
     * 点赞时向评论作者发送通知（不通知自己）。
     *
     * @param commentId   评论 ID
     * @param currentUser 当前用户
     * @return true=已点赞，false=已取消
     */
    @Transactional
    public boolean toggleCommentLike(Long commentId, User currentUser) {
        Comment comment = commentRepository.findByIdWithPostAndUser(commentId)
                .orElseThrow(() -> new NotFoundException("评论不存在"));

        Optional<UserLike> existing = userLikeRepository.findByUserIdAndTargetTypeAndTargetId(
                currentUser.getId(), UserLike.TargetType.COMMENT, commentId);

        if (existing.isPresent()) {
            userLikeRepository.delete(existing.get());
            comment.setLikeCount(Math.max(0, comment.getLikeCount() - 1));
            commentRepository.save(comment);
            return false;
        } else {
            UserLike like = new UserLike();
            like.setUserId(currentUser.getId());
            like.setTargetType(UserLike.TargetType.COMMENT);
            like.setTargetId(commentId);
            userLikeRepository.save(like);
            comment.setLikeCount(comment.getLikeCount() + 1);
            commentRepository.save(comment);

            Long commentAuthorId = comment.getUser().getId();
            if (!commentAuthorId.equals(currentUser.getId())) {
                notificationService.send(commentAuthorId, Notification.NotificationType.LIKE,
                        currentUser.getNickname() + " 赞了你的评论",
                        "/posts/" + comment.getPost().getId());
            }

            return true;
        }
    }

    public Page<Comment> getUserComments(Long userId, int page) {
        Pageable pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        return commentRepository.findByUserId(userId, pageable);
    }

    public boolean isCommentLikedByUser(Long commentId, Long userId) {
        return userLikeRepository.existsByUserIdAndTargetTypeAndTargetId(
                userId, UserLike.TargetType.COMMENT, commentId);
    }

    public Set<Long> getLikedCommentIdsByUser(List<Long> commentIds, Long userId) {
        if (commentIds.isEmpty()) return Set.of();
        return userLikeRepository.findByUserIdAndTargetTypeAndTargetIdIn(userId, UserLike.TargetType.COMMENT, commentIds)
                .stream().map(UserLike::getTargetId).collect(Collectors.toSet());
    }
}
