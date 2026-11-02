package com.restond.echobbs.module.board.service;

import com.restond.echobbs.common.ForbiddenException;
import com.restond.echobbs.common.NotFoundException;
import com.restond.echobbs.module.board.model.Board;
import com.restond.echobbs.module.board.repository.BoardRepository;
import com.restond.echobbs.module.post.repository.PostRepository;
import com.restond.echobbs.module.user.model.User;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * 板块业务服务。
 * <p>
 * 管理板块的增删改查。创建、编辑、删除操作仅限 ADMIN 角色。
 * 删除板块前会检查是否有关联帖子，存在帖子时拒绝删除。
 */
@Service
public class BoardService {

    private final BoardRepository boardRepository;
    private final PostRepository postRepository;

    public BoardService(BoardRepository boardRepository, PostRepository postRepository) {
        this.boardRepository = boardRepository;
        this.postRepository = postRepository;
    }

    public List<Board> getAllBoards() {
        return boardRepository.findAllByOrderBySortOrderAsc();
    }

    public Board getBoardBySlug(String slug) {
        return boardRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("板块不存在"));
    }

    @Transactional
    @CacheEvict(value = "boards", allEntries = true )
    public Board createBoard(String name, String slug, String description, Integer sortOrder, User currentUser) {
        if (currentUser.getRole() != User.UserRole.ADMIN) {
            throw new ForbiddenException("无权限执行此操作");
        }

        if (boardRepository.findBySlug(slug).isPresent()) {
            throw new IllegalArgumentException("板块标识已存在");
        }

        Board board = new Board();
        board.setName(name);
        board.setSlug(slug);
        board.setDescription(description);
        board.setSortOrder(sortOrder != null ? sortOrder : 0);

        return boardRepository.save(board);
    }

    @Transactional
    @CacheEvict(value = "boards", allEntries = true )
    public Board updateBoard(Long boardId, String name, String slug, String description, Integer sortOrder, User currentUser) {
        if (currentUser.getRole() != User.UserRole.ADMIN) {
            throw new ForbiddenException("无权限执行此操作");
        }

        Board board = boardRepository.findById(boardId).orElseThrow(() -> new NotFoundException("板块不存在"));

        if (!board.getSlug().equals(slug)) {
            boardRepository.findBySlug(slug).ifPresent(existingBoard -> {
                if(!existingBoard.getId().equals(boardId)) {
                    throw new IllegalArgumentException("板块标识已使用");
                }
            });
        }

        board.setName(name);
        board.setSlug(slug);
        board.setDescription(description);
        board.setSortOrder(sortOrder != null ? sortOrder : board.getSortOrder());

        return boardRepository.save(board);
    }

    @Transactional
    @CacheEvict(value = "boards", allEntries = true )
    public void deleteBoard(Long boardId, User currentUser) {
        if (currentUser.getRole() != User.UserRole.ADMIN) {
            throw new ForbiddenException("无权限执行此操作");
        }

        Board board = boardRepository.findById(boardId).orElseThrow(() -> new NotFoundException("板块不存在"));

        long postCount = postRepository.countByBoardId(boardId);
        if (postCount > 0) {
            throw new IllegalArgumentException("该板块下还有 " + postCount + " 个帖子，无法删除");
        }

        boardRepository.delete(board);
    }
}
