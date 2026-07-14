package norimaets.appapiserver.domain.auth.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        Long userId,
        String username,
        String avatarUrl
) {
}
