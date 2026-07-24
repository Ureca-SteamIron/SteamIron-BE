package norimaets.appnotificationserver.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import norimaets.appnotificationserver.dto.UnreadNotificationCountResponse;
import norimaets.appnotificationserver.dto.UserNotificationCreateRequest;
import norimaets.appnotificationserver.dto.UserNotificationCreateResponse;
import norimaets.appnotificationserver.dto.UserNotificationCreateStatus;
import norimaets.appnotificationserver.dto.UserNotificationPageResponse;
import norimaets.appnotificationserver.dto.UserNotificationResponse;
import norimaets.appnotificationserver.service.UserNotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/notifications")
@RequiredArgsConstructor
public class UserNotificationController {

    private final UserNotificationService userNotificationService;

    @PostMapping
    public UserNotificationCreateResponse create(
            @Valid @RequestBody UserNotificationCreateRequest request
    ) {
        UserNotificationCreateStatus status =
                userNotificationService.create(request);

        return new UserNotificationCreateResponse(status);
    }

    @GetMapping
    public UserNotificationPageResponse getNotifications(
            @RequestParam Long userId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<UserNotificationResponse> notifications =
                userNotificationService.getNotifications(userId, pageable);

        return UserNotificationPageResponse.from(notifications);
    }

    @GetMapping("/unread-count")
    public UnreadNotificationCountResponse getUnreadCount(
            @RequestParam Long userId
    ) {
        long unreadCount =
                userNotificationService.getUnreadCount(userId);

        return new UnreadNotificationCountResponse(unreadCount);
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable String notificationId,
            @RequestParam Long userId
    ) {
        boolean updated =
                userNotificationService.markAsRead(notificationId, userId);

        if (!updated) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.noContent().build();
    }

    // 회원 탈퇴 시 api-server가 호출: 해당 유저의 알림 데이터를 전부 삭제
    @DeleteMapping
    public ResponseEntity<Void> deleteAllByUserId(
            @RequestParam Long userId
    ) {
        userNotificationService.deleteAllByUserId(userId);
        return ResponseEntity.noContent().build();
    }
}
