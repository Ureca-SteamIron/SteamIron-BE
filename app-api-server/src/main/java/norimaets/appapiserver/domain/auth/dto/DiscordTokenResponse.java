package norimaets.appapiserver.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Discord 토큰 API(POST /api/oauth2/token)의 응답.
 * Discord는 snake_case로 응답하므로 @JsonProperty로 매핑한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DiscordTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("scope") String scope
) {
}
