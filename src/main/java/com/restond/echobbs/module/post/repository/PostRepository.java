package com.restond.echobbs.module.post.repository;

import com.restond.echobbs.module.post.model.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * 帖子数据访问层。
 * <p>
 * 使用 JPQL + JOIN FETCH 解决懒加载问题（open-in-view 已关闭），
 * 避免事务外访问关联实体时抛出 LazyInitializationException。
 */
@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    @Query(value = "SELECT p FROM Post p JOIN FETCH p.board b JOIN FETCH p.user u " +
            "WHERE b.slug = :slug AND p.status = :status",
            countQuery = "SELECT COUNT(p) FROM Post p JOIN p.board b " +
                    "WHERE b.slug = :slug AND p.status = :status")
    Page<Post> findByBoardSlugAndStatus(@Param("slug") String slug,
                                        @Param("status") Post.PostStatus status,
                                        Pageable pageable);

    @Query("SELECT p FROM Post p JOIN FETCH p.board JOIN FETCH p.user WHERE p.id = :id AND p.status = :status")
    Optional<Post> findByIdAndStatusWithDetails(@Param("id") Long id, @Param("status") Post.PostStatus status);

    @Query("SELECT p FROM Post p JOIN FETCH p.board JOIN FETCH p.user WHERE p.id = :id")
    Optional<Post> findByIdWithDetails(@Param("id") Long id);

    @Query("""
    SELECT p FROM Post p
    JOIN FETCH p.board
    JOIN FETCH p.user
    WHERE (title || ' ' || content) ILIKE :keyword
    ORDER BY p.createdAt DESC
    """)
    Page<Post> searchPosts(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
    SELECT p FROM Post p
    JOIN FETCH p.board
    JOIN FETCH p.user
    WHERE p.user.id = :userId
    AND p.status = 'NORMAL'
    ORDER BY p.createdAt DESC
    """)
    Page<Post> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("""
    SELECT p FROM Post p
    JOIN FETCH p.board
    JOIN FETCH p.user
    WHERE p.status = 'NORMAL'
    ORDER BY p.createdAt DESC
    """)
    Page<Post> findLatestPosts(Pageable pageable);

    @Query("""
    SELECT p FROM Post p
    JOIN FETCH p.board
    JOIN FETCH p.user
    WHERE p.status = 'NORMAL'
    ORDER BY p.viewCount DESC, p.createdAt DESC
    """)
    Page<Post> findHotPosts(Pageable pageable);

    @Query("SELECT COUNT(p) FROM Post p WHERE p.board.id = :boardId")
    long countByBoardId(@Param("boardId") Long boardId);

    @Query(value = "SELECT p FROM Post p JOIN FETCH p.board JOIN FETCH p.user WHERE p.status = :status",
            countQuery = "SELECT COUNT(p) FROM Post p WHERE p.status = :status")
    Page<Post> findByStatus(@Param("status") Post.PostStatus status, Pageable pageable);
}
