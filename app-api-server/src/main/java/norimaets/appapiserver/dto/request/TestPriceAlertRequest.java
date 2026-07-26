package norimaets.appapiserver.dto.request;

/**
 * 카프카 가격 알림 이벤트 발행 테스트용 요청. notificationType은 "DISCOUNT_START" 또는 "TARGET_PRICE".
 * discordUserId를 비워두면 알림 서버가 Discord DM 없이 웹 알림만 처리한다.
 */
public record TestPriceAlertRequest(
        Long alertId,
        Long userId,
        String discordUserId,
        Long gameId,
        String gameName,
        Integer targetPrice,
        Integer currentPrice,
        Integer discountPercent,
        String notificationType
) {
}
