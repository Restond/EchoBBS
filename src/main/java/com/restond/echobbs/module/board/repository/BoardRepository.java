package com.restond.echobbs.module.board.repository;

import com.restond.echobbs.module.board.model.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {
    List<Board> findAllByOrderBySortOrderAsc();
    Optional<Board> findBySlug(String slug);
}
