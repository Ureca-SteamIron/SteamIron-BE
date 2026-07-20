package norimaets.appapiserver.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import norimaets.appapiserver.common.response.ApiResponse;
import norimaets.appapiserver.dto.request.DiscordNotificationSettingRequest;
import norimaets.appapiserver.security.LoginUserId;
import norimaets.appapiserver.service.DiscordNotificationSettingService;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users/me/notification-settings")
public class DiscordNotificationSettingController {

    private final DiscordNotificationSettingService notificationSettingService;

    @PatchMapping("/discord")
    public ApiResponse<Void> updateDiscordNotificationSetting(
            @LoginUserId Long userId,
            @Valid @RequestBody DiscordNotificationSettingRequest request
    ) {
        notificationSettingService.updateSetting(userId, request.enabled());
        return ApiResponse.success();
    }
}