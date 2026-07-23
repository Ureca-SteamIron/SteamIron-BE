package norimaets.appnotificationserver.service;

import lombok.RequiredArgsConstructor;
import norimaets.appnotificationserver.dto.UserNotificationCreateRequest;
import norimaets.appnotificationserver.dto.UserNotificationCreateStatus;
import norimaets.appnotificationserver.dto.UserNotificationResponse;
import norimaets.moduledomainmongo.document.UserNotification;
import norimaets.moduledomainmongo.repository.UserNotificationRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserNotificationService {

    private final UserNotificationRepository userNotificationRepository;

    public UserNotificationCreateStatus create(
            UserNotificationCreateRequest request
    ) {
        if (userNotificationRepository.findByEventKey(request.eventKey()).isPresent()) {
            return UserNotificationCreateStatus.ALREADY_EXISTS;
        }

        UserNotification notification = UserNotification.builder()
                .eventKey(request.eventKey())
                .notificationType(request.notificationType())
                .alertId(request.alertId())
                .userId(request.userId())
                .gameId(request.gameId())
                .gameName(request.gameName())
                .targetPrice(request.targetPrice())
                .currentPrice(request.currentPrice())
                .discountPercent(request.discountPercent())
                .build();

        try {
            userNotificationRepository.save(notification);
            return UserNotificationCreateStatus.CREATED;
        } catch (DuplicateKeyException e) {
            return UserNotificationCreateStatus.ALREADY_EXISTS;
        }
    }

    public Page<UserNotificationResponse> getNotifications(
            Long userId,
            Pageable pageable
    ) {
        return userNotificationRepository
                .findAllByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(UserNotificationResponse::from);
    }

    public boolean markAsRead(
            String notificationId,
            Long userId
    ) {
        UserNotification notification = userNotificationRepository
                .findByIdAndUserId(notificationId, userId)
                .orElse(null);

        if (notification == null) {
            return false;
        }

        notification.markRead();
        userNotificationRepository.save(notification);
        return true;
    }

    public long getUnreadCount(Long userId) {
        return userNotificationRepository.countByUserIdAndReadFalse(userId);
    }
}
