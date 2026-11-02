package com.restond.echobbs.module.board.controller.api;

import com.restond.echobbs.common.Result;
import com.restond.echobbs.module.board.model.Board;
import com.restond.echobbs.module.board.model.dto.BoardResponse;
import com.restond.echobbs.module.board.model.dto.CreateBoardRequest;
import com.restond.echobbs.module.board.model.dto.UpdateBoardRequest;
import com.restond.echobbs.module.board.service.BoardService;
import com.restond.echobbs.module.user.model.User;
import com.restond.echobbs.module.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * 板块管理 API 控制器（管理员专用）。
 * <p>
 * 提供板块的增删改接口，需要 ADMIN 或 MODERATOR 角色。
 * <ul>
 *   <li>POST   /api/admin/boards - 创建板块</li>
 *   <li>PUT    /api/admin/boards/{id} - 编辑板块</li>
 *   <li>DELETE /api/admin/boards/{id} - 删除板块（有关联帖子时拒绝）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin/boards")
public class BoardAdminApiController {
    private final BoardService boardService;
    private final UserService userService;

    public BoardAdminApiController(BoardService boardService, UserService userService) {
        this.boardService = boardService;
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<Result<BoardResponse>> createBoards(@Valid @RequestBody CreateBoardRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByUsername(userDetails.getUsername());

        Board board = boardService.createBoard(
                request.name(),
                request.slug(),
                request.description(),
                request.sortOrder(),
                user
        );

        return ResponseEntity.ok(Result.ok(BoardResponse.from(board)));
    }

    @PutMapping("{id}")
    public ResponseEntity<Result<BoardResponse>> updateBoard(@PathVariable Long id, @Valid @RequestBody UpdateBoardRequest request, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByUsername(userDetails.getUsername());

        Board board = boardService.updateBoard(
                id,
                request.name(),
                request.slug(),
                request.description(),
                request.sortOrder(),
                user
        );

        return ResponseEntity.ok(Result.ok(BoardResponse.from(board)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Result<Void>> deleteBoard(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.getUserByUsername(userDetails.getUsername());

        boardService.deleteBoard(id, user);
        return ResponseEntity.ok(Result.ok(null));
    }
}
