package norimaets.appapiserver.dto.request;

import jakarta.validation.constraints.NotNull;

public record DiscordNotificationSettingRequest(
        @NotNull(message = "Discord 알림 설정 여부를 설정해주세요.")
        Boolean enabled
) {
}