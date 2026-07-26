package norimaets.appapiserver.dto.notification;

/**
 * app-batch-server의 PriceAlertNotificationEvent와 동일한 스키마.
 * app-api-server는 배치 모듈에 의존하지 않는 별도 배포 단위라 카프카 페이로드 호환을 위해 필드를 그대로 복제한다.
 */
public record PriceAlertNotificationEvent(
        String eventKey,
        String notificationType,
        Long alertId,
        Long userId,
        String discordUserId,
        Long gameId,
        String gameName,
        Integer targetPrice,
        Integer currentPrice,
        Integer discountPercent
) {
}
