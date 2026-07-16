package norimaets.appapiserver.dto;

public record ReissueResponse(
        String accessToken,
        String refreshToken
) {
}
