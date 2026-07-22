package norimaets.appnotificationserver.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 배치 서버 → 알림 서버 Discord DM 발송 요청. (계약: POST /internal/v1/discord/dms)
 * eventKey 형식: price-alert:{alertId}:{currentPrice} — 같은 알림 중복 발송 방지 키.
 */
public record DiscordDmRequest(
        @NotBlank String eventKey,
        Long alertId,
        Long userId,
        @NotBlank String discordUserId,
        Long gameId,
        String gameName,
        Integer targetPrice,
        Integer currentPrice,
        Integer discountPercent
) {
}
