package com.restond.echobbs.module.notification.repository;

import com.restond.echobbs.module.notification.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * 通知数据访问层。
 * <p>
 * {@link #markAllReadByUserId} 使用批量 UPDATE 语句一次性标记已读，避免逐条更新。
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    long countByUserIdAndIsRead(Long userId, boolean isRead);

    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.userId = :userId AND n.isRead = false")
    void markAllReadByUserId(Long userId);
}
