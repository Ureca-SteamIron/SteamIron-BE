package norimaets.appnotificationserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import norimaets.appnotificationserver.dto.UserNotificationCreateRequest;
import norimaets.appnotificationserver.dto.UserNotificationCreateStatus;
import norimaets.appnotificationserver.dto.UserNotificationResponse;
import norimaets.moduledomainmongo.document.UserNotification;
import norimaets.moduledomainmongo.repository.DiscordDeliveryLogRepository;
import norimaets.moduledomainmongo.repository.UserNotificationRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserNotificationService {

    private final UserNotificationRepository userNotificationRepository;
    private final DiscordDeliveryLogRepository discordDeliveryLogRepository;

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

    // 회원 탈퇴 시: 해당 유저의 알림/디스코드 발송 로그를 Mongo에서 전부 삭제.
    // api-server가 탈퇴 처리 도중 베스트 에포트로 호출한다(실패해도 탈퇴 자체는 진행).
    public void deleteAllByUserId(Long userId) {
        userNotificationRepository.deleteAllByUserId(userId);
        discordDeliveryLogRepository.deleteAllByUserId(userId);
        log.info("유저 알림 데이터 삭제 완료: userId={}", userId);
    }
}
