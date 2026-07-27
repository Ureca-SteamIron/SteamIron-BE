package norimaets.appnotificationserver.dto;

/**
 * 배치 → 알림 서버로 발행되는 통합 알림 이벤트.
 * 알림 서버 컨슈머가 이 이벤트 하나로 "웹 알림 저장"과 "Discord DM 발송(선택)"을 모두 처리한다.
 *
 * discordUserId: null이면 Discord DM을 보내지 않는다(유저가 Discord 알림을 껐거나 discordId가 없는 경우).
 *                배치 서버만 User/PriceAlert(Postgres)에 접근할 수 있어, 이 판단은 여기서 미리 끝내고
 *                알림 서버는 Postgres를 조회하지 않고도 이벤트만으로 처리를 완결할 수 있게 한다.
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