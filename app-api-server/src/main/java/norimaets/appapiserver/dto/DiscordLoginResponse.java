package norimaets.appapiserver.dto;

import norimaets.moduledomainrdb.entity.Role;

public record DiscordLoginResponse(
        boolean accountSetupRequired,
        String accountSetupToken,
        String accessToken,
        String refreshToken,
        Long userId,
        String nickname,
        String avatarUrl,
        Role role
) {
    public static DiscordLoginResponse accountSetupRequired(String accountSetupToken) {
        return new DiscordLoginResponse(
                true, accountSetupToken,
                null, null, null, null, null, null
        );
    }

    public static DiscordLoginResponse loggedIn(LoginResponse login) {
        return new DiscordLoginResponse(
                false, null,
                login.accessToken(), login.refreshToken(), login.userId(),
                login.nickname(), login.avatarUrl(), login.role()
        );
    }
}
