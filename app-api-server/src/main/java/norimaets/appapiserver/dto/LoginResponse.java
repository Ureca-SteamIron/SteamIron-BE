package norimaets.appapiserver.dto;

import norimaets.moduledomainrdb.entity.Role;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        Long userId,
        String nickname,
        String avatarUrl,
        Role role
) {
}
