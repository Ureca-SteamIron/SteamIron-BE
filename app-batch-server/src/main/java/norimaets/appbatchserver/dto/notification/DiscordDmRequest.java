package norimaets.appbatchserver.dto.notification;

public record DiscordDmRequest(
        String eventKey,
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