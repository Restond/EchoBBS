package com.restond.echobbs.module.like.repository;

import com.restond.echobbs.module.like.model.UserLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserLikeRepository extends JpaRepository<UserLike, Long> {

    Optional<UserLike> findByUserIdAndTargetTypeAndTargetId(
            Long userId, UserLike.TargetType targetType, Long targetId);

    boolean existsByUserIdAndTargetTypeAndTargetId(
            Long userId, UserLike.TargetType targetType, Long targetId);

    List<UserLike> findByUserIdAndTargetTypeAndTargetIdIn(
            Long userId, UserLike.TargetType targetType, Collection<Long> targetIds);
}
