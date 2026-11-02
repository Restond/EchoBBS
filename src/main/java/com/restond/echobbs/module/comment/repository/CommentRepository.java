package com.restond.echobbs.module.comment.repository;

import com.restond.echobbs.module.comment.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

/**
 * 评论数据访问层。
 * <p>
 * 查询策略：
 * <ul>
 *   <li>{@link #findByPostIdFlattened} - 按 rootId 分组、时间排序的扁平列表，前端组装层级</li>
 *   <li>{@link #findByIdWithDetails} - 回复评论时使用，JOIN FETCH post + board + user 避免懒加载</li>
 * </ul>
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    @Query("SELECT c FROM Comment c JOIN FETCH c.user JOIN FETCH c.post p JOIN FETCH p.board " +
            "WHERE c.post.id = :postId AND c.status = 'NORMAL' " +
            "ORDER BY COALESCE(c.rootId, c.id), c.createdAt")
    List<Comment> findByPostIdFlattened(@Param("postId") Long postId);

    @Query("SELECT c FROM Comment c JOIN FETCH c.user JOIN FETCH c.post p JOIN FETCH p.user JOIN FETCH p.board " +
            "WHERE c.id = :id AND c.status = 'NORMAL'")
    Optional<Comment> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT c FROM Comment c JOIN FETCH c.post JOIN FETCH c.user WHERE c.id = :id")
    Optional<Comment> findByIdWithPostAndUser(Long id);

    @Query("""
    SELECT c FROM Comment c
    JOIN FETCH c.post p
    JOIN FETCH p.board
    JOIN FETCH p.user
    JOIN FETCH c.user
    WHERE c.user.id = :userId
    AND c.status = 'NORMAL'
    ORDER BY c.createdAt DESC
    """)
    Page<Comment> findByUserId(@Param("userId") Long userId, Pageable pageable);
}
