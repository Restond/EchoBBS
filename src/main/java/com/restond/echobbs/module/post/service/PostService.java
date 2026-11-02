package com.restond.echobbs.module.post.service;

import com.restond.echobbs.common.ForbiddenException;
import com.restond.echobbs.common.NotFoundException;
import com.restond.echobbs.module.board.model.Board;
import com.restond.echobbs.module.board.repository.BoardRepository;
import com.restond.echobbs.module.like.model.UserLike;
import com.restond.echobbs.module.like.repository.UserLikeRepository;
import com.restond.echobbs.module.notification.model.Notification;
import com.restond.echobbs.module.notification.service.NotificationService;
import com.restond.echobbs.module.post.model.Post;
import com.restond.echobbs.module.post.model.dto.CreatePostRequest;
import com.restond.echobbs.module.post.repository.PostRepository;
import com.restond.echobbs.module.user.model.User;
import org.hibernate.annotations.NotFound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

/**
 * 帖子业务服务。
 * <p>
 * 处理帖子的创建、编辑、删除、点赞、搜索、置顶、锁定等核心业务逻辑。
 * 热门帖子和最新帖子列表通过 Caffeine 缓存加速，写操作时自动失效缓存。
 * 浏览量统计委托给 {@link ViewCountService} 异步批量处理。
 */
@Service
public class PostService {
    private static final Logger log = LoggerFactory.getLogger(PostService.class);

    private final PostRepository postRepository;
    private final BoardRepository boardRepository;
    private final UserLikeRepository userLikeRepository;
    private final NotificationService notificationService;
    private final ViewCountService viewCountService;

    public PostService(PostRepository postRepository, BoardRepository boardRepository,
                       UserLikeRepository userLikeRepository, NotificationService notificationService,
                       ViewCountService viewCountService) {
        this.postRepository = postRepository;
        this.boardRepository = boardRepository;
        this.userLikeRepository = userLikeRepository;
        this.notificationService = notificationService;
        this.viewCountService = viewCountService;
    }

    @Transactional
    @CacheEvict(value = {"hotPosts", "latestPosts"}, allEntries = true)
    public Post createPost(User user, CreatePostRequest request) {
        Board board = boardRepository.findById(request.boardId())
                .orElseThrow(() -> new IllegalArgumentException("板块不存在"));

        Post post = new Post();
        post.setBoard(board);
        post.setUser(user);
        post.setTitle(request.title());
        post.setContent(request.content());

        return postRepository.save(post);
    }

    public Page<Post> getPostsByBoardSlug(String slug, int page) {
        Pageable pageable = PageRequest.of(page, 20,
                Sort.by(Sort.Order.desc("isPinned"), Sort.Order.desc("lastRepliedAt")));
        return postRepository.findByBoardSlugAndStatus(slug, Post.PostStatus.NORMAL, pageable);
    }

    public Optional<Post> getPostById(Long id) {
        return postRepository.findByIdAndStatusWithDetails(id, Post.PostStatus.NORMAL);
    }

    /**
     * 编辑帖子。
     * <p>
     * 仅帖子作者可编辑，非作者抛出 ForbiddenException。
     */
    @Transactional
    public Post updatePost(Long postId, String title, String content, Long currentUserId) {
        Post post = postRepository.findByIdAndStatusWithDetails(postId, Post.PostStatus.NORMAL)
                .orElseThrow(() -> new NotFoundException("帖子不存在"));

        if (!post.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException("无权编辑此帖子");
        }

        post.setTitle(title);
        post.setContent(content);
        return postRepository.save(post);
    }

    /**
     * 删除帖子（软删除）。
     * <p>
     * 权限规则：帖子作者或管理员（ADMIN/MODERATOR）可删除。
     */
    @Transactional
    public void deletePost(Long postId, User currentUser) {
        Post post = postRepository.findByIdAndStatusWithDetails(postId, Post.PostStatus.NORMAL)
                .orElseThrow(() -> new NotFoundException("帖子不存在"));

        boolean isOwner = post.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == User.UserRole.ADMIN
                || currentUser.getRole() == User.UserRole.MODERATOR;

        if (!isOwner && !isAdmin) {
            throw new ForbiddenException("无权删除此帖子");
        }

        post.setStatus(Post.PostStatus.DELETED);
        postRepository.save(post);
    }

    public void recordView(Long postId) {
        viewCountService.recordView(postId);
    }

    public Optional<Post> getPostByIdAnyStatus(Long id) {
        return postRepository.findByIdWithDetails(id);
    }

    /**
     * 切换帖子点赞状态。
     * <p>
     * 已点赞则取消，未点赞则新增。点赞时向帖子作者发送通知（不通知自己）。
     *
     * @return true=已点赞，false=已取消
     */
    @Transactional
    public boolean togglePostLike(Long postId, User currentUser) {
        Post post = postRepository.findByIdWithDetails(postId)
                .orElseThrow(() -> new NotFoundException("帖子不存在"));

        Optional<UserLike> existing = userLikeRepository.findByUserIdAndTargetTypeAndTargetId(
                currentUser.getId(), UserLike.TargetType.POST, postId);

        if (existing.isPresent()) {
            userLikeRepository.delete(existing.get());
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
            postRepository.save(post);
            return false;
        } else {
            UserLike like = new UserLike();
            like.setUserId(currentUser.getId());
            like.setTargetType(UserLike.TargetType.POST);
            like.setTargetId(postId);
            userLikeRepository.save(like);
            post.setLikeCount(post.getLikeCount() + 1);
            postRepository.save(post);

            Long postAuthorId = post.getUser().getId();
            if (!postAuthorId.equals(currentUser.getId())) {
                notificationService.send(postAuthorId, Notification.NotificationType.LIKE,
                        currentUser.getNickname() + " 赞了你的帖子《" + post.getTitle() + "》",
                        "/posts/" + postId);
            }

            return true;
        }
    }

    public Page<Post> searchPosts(String keyword, int page) {
        if (keyword == null || keyword.isBlank()) {
            return Page.empty();
        }

        // 转义 LIKE 通配符，防止 SQL 模式注入
        String escapedKeyword = keyword.trim()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");

        String pattern = "%" + escapedKeyword + "%";

        Pageable pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        return postRepository.searchPosts(pattern, pageable);
    }

    public Page<Post> getUserPosts(Long userId, int page) {
        Pageable pageable = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        return postRepository.findByUserId(userId, pageable);
    }

    public boolean isPostLikedByUser(Long postId, Long userId) {
        return userLikeRepository.existsByUserIdAndTargetTypeAndTargetId(
                userId, UserLike.TargetType.POST, postId);
    }

    @Cacheable(value = "latestPosts", key = "#limit")
    public List<Post> getLatestPosts(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return postRepository.findLatestPosts(pageable).getContent();
    }

    @Cacheable(value = "hotPosts", key = "#limit")
    public List<Post> getHotPosts(int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "viewCount", "createdAt"));
        return postRepository.findHotPosts(pageable).getContent();
    }

    public Page<Post> getPosts(int page, int size, String sort, String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            return searchPosts(keyword, page);
        }

        Sort sortOrder = switch (sort == null ? "latest" : sort) {
            case "views" -> Sort.by(Sort.Direction.DESC, "isPinned", "viewCount", "createdAt");
            case "likes" -> Sort.by(Sort.Direction.DESC, "isPinned", "likeCount", "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "isPinned", "createdAt");
        };

        Pageable pageable = PageRequest.of(page, size, sortOrder);
        return postRepository.findByStatus(Post.PostStatus.NORMAL, pageable);
    }

    @Transactional
    @CacheEvict(value = {"hotPosts", "latestPosts"}, allEntries = true)
    public Post togglePin(Long postId, User currentUser) {
        if (currentUser.getRole() != User.UserRole.ADMIN && currentUser.getRole() != User.UserRole.MODERATOR) {
            throw new ForbiddenException("无权限执行此操作");
        }

        Post post = postRepository.findByIdWithDetails(postId).orElseThrow(() -> new NotFoundException("帖子不存在"));

        boolean newPinStatus = !post.getIsPinned();
        post.setIsPinned(!post.getIsPinned());

        extracted(postId, currentUser, newPinStatus, post);

        return postRepository.save(post);
    }

    @Transactional
    @CacheEvict(value = {"hotPosts", "latestPosts"}, allEntries = true)
    public Post toggleLock(Long postId, User currentUser) {
        if (currentUser.getRole() != User.UserRole.ADMIN && currentUser.getRole() != User.UserRole.MODERATOR) {
            throw new ForbiddenException("无权限执行此操作");
        }

        Post post = postRepository.findByIdWithDetails(postId).orElseThrow(() -> new NotFoundException("帖子不存在"));
        boolean newLockStatus = !post.getIsLocked();

        post.setIsLocked(!post.getIsLocked());

        extracted(postId, currentUser, newLockStatus, post);

        return postRepository.save(post);
    }

    private static void extracted(Long postId, User currentUser, boolean newLockStatus, Post post) {
        log.info("管理员操作 操作者={}, 操作={}, 帖子ID={}, 标题={}",
                currentUser.getUsername(), newLockStatus ? "锁定" : "解锁", postId, post.getTitle());
    }

    public Page<Post> getPostsByUser(Long userId, int page) {
        return postRepository.findByUserId(userId, PageRequest.of(page, 20, Sort.by("createdAt").descending()));
    }
}
