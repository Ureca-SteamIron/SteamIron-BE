package norimaets.appapiserver.dto.response;

import java.time.Instant;

public record UserNotificationResponse(
        String id,
        String notificationType,
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
}
