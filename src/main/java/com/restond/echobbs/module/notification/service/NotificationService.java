package com.restond.echobbs.module.notification.service;

import com.restond.echobbs.module.notification.model.Notification;
import com.restond.echobbs.module.notification.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 通知业务服务。
 * <p>
 * 负责通知的发送、查询和标记已读。通知在评论回复、帖子/评论点赞时自动触发。
 */
@Service
public class NotificationService {
    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void send(Long recipientUserId, Notification.NotificationType type,String content, String targetUrl) {
        Notification n = new Notification();
        n.setUserId(recipientUserId);
        n.setType(type);
        n.setContent(content);
        n.setTargetUrl(targetUrl);
        notificationRepository.save(n);
    }

    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndIsRead(userId, false);
    }

    public Page<Notification> getNotifications(Long userId, int page) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, 20));
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllReadByUserId(userId);
    }
}
