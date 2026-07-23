package norimaets.appbatchserver.dto.notification;

public record UserNotificationCreateRequest(
        String eventKey,
        String notificationType,
        Long alertId,
        Long userId,
        Long gameId,
        String gameName,
        Integer targetPrice,
        Integer currentPrice,
        Integer discountPercent
) {
}
