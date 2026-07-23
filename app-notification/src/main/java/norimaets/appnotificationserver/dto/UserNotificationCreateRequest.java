package norimaets.appnotificationserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import norimaets.moduledomainmongo.document.UserNotificationType;

public record UserNotificationCreateRequest(
        @NotBlank String eventKey,
        @NotNull UserNotificationType notificationType,
        Long alertId,
        @NotNull Long userId,
        @NotNull Long gameId,
        @NotBlank String gameName,
        Integer targetPrice,
        @NotNull Integer currentPrice,
        Integer discountPercent
) {
}