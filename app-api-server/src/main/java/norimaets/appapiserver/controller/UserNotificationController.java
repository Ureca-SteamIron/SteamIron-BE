package norimaets.appapiserver.controller;

import lombok.RequiredArgsConstructor;
import norimaets.appapiserver.common.response.ApiResponse;
import norimaets.appapiserver.dto.response.UnreadNotificationCountResponse;
import norimaets.appapiserver.dto.response.UserNotificationPageResponse;
import norimaets.appapiserver.security.LoginUserId;
import norimaets.appapiserver.service.UserNotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me/notifications")
public class UserNotificationController {

    private final UserNotificationService userNotificationService;

    @GetMapping
    public ApiResponse<UserNotificationPageResponse> getNotifications(
            @LoginUserId Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(
                userNotificationService.getNotifications(userId, page, size)
        );
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadNotificationCountResponse> getUnreadCount(
            @LoginUserId Long userId
    ) {
        return ApiResponse.success(
                userNotificationService.getUnreadCount(userId)
        );
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<Void> markAsRead(
            @LoginUserId Long userId,
            @PathVariable String notificationId
    ) {
        userNotificationService.markAsRead(notificationId, userId);
        return ApiResponse.success();
    }
}
