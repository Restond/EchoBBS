package com.restond.echobbs.module.board.controller.api;

import com.restond.echobbs.common.Result;
import com.restond.echobbs.module.board.model.dto.BoardResponse;
import com.restond.echobbs.module.board.service.BoardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 板块 API 控制器。
 * <p>
 * 提供板块列表查询接口（公开），供前端导航和发帖选择使用。
 */
@RestController
@RequestMapping("/api/boards")
public class BoardApiController {

    private final BoardService boardService;

    public BoardApiController(BoardService boardService) {
        this.boardService = boardService;
    }

    @GetMapping
    public ResponseEntity<Result<List<BoardResponse>>> getBoards() {
        List<BoardResponse> boards = boardService.getAllBoards().stream()
                .map(BoardResponse::from)
                .toList();
        return ResponseEntity.ok(Result.ok(boards));
    }
}
