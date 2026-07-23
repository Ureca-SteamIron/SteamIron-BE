package norimaets.appnotificationserver.dto;

import java.time.Instant;
import norimaets.moduledomainmongo.document.UserNotification;
import norimaets.moduledomainmongo.document.UserNotificationType;

public record UserNotificationResponse(
        String id,
        UserNotificationType notificationType,
        Long alertId,
        Long gameId,
        String gameName,
        Integer targetPrice,
        Integer currentPrice,
        Integer discountPercent,
        boolean read,
        Instant createdAt,
        Instant readAt
) {

    public static UserNotificationResponse from(
            UserNotification notification
    ) {
        return new UserNotificationResponse(
                notification.getId(),
                notification.getNotificationType(),
                notification.getAlertId(),
                notification.getGameId(),
                notification.getGameName(),
                notification.getTargetPrice(),
                notification.getCurrentPrice(),
                notification.getDiscountPercent(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }
}