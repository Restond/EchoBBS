package com.restond.echobbs.module.notification.controller.api;

import com.restond.echobbs.common.Result;
import com.restond.echobbs.module.notification.model.Notification;
import com.restond.echobbs.module.notification.model.dto.NotificationResponse;
import com.restond.echobbs.module.notification.service.NotificationService;
import com.restond.echobbs.module.user.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通知 API 控制器。
 * <p>
 * 提供通知列表、未读数查询和批量标记已读接口。
 * 未读数接口（{@code /unread-count}）对游客返回 0，无需认证。
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationApiController {
    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationApiController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<Result<Page<NotificationResponse>>> list(
            @RequestParam(defaultValue = "0") int page) {
        return userService.getCurrentUser()
                .map(u -> {
                    Page<Notification> notifications = notificationService.getNotifications(u.getId(), page);
                    Page<NotificationResponse> responses = notifications.map(NotificationResponse::from);
                    return ResponseEntity.ok(Result.ok(responses));
                })
                .orElseGet(() -> ResponseEntity.status(401).body(Result.error(401, "未登录")));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Result<Long>> unreadCount() {
        return userService.getCurrentUser()
                .map(u -> ResponseEntity.ok(Result.ok(notificationService.countUnread(u.getId()))))
                .orElseGet(() -> ResponseEntity.ok(Result.ok(0L)));
    }

    @PostMapping("/read-all")
    public ResponseEntity<Result<Void>> markAllRead() {
        userService.getCurrentUser().ifPresent(u -> notificationService.markAllRead(u.getId()));
        return ResponseEntity.ok(Result.ok());
    }
}
