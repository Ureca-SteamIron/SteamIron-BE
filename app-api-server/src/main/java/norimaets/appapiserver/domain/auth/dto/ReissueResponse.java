package norimaets.appapiserver.domain.auth.dto;

public record ReissueResponse(
        String accessToken,
        String refreshToken
) {
}
