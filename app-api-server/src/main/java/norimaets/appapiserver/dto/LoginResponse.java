package norimaets.appapiserver.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        Long userId,
        String username,
        String avatarUrl
) {
}
