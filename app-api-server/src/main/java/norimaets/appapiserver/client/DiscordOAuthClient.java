package norimaets.appapiserver.client;

import norimaets.appapiserver.dto.DiscordTokenResponse;
import norimaets.appapiserver.dto.DiscordUserResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Discord OAuth2 API와 통신하는 클라이언트.
 * ① code → access_token 교환, ② access_token → 유저 정보 조회
 */
@Component
public class DiscordOAuthClient {

    private static final String TOKEN_URL = "https://discord.com/api/oauth2/token";
    private static final String USER_URL = "https://discord.com/api/users/@me";

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public DiscordOAuthClient(
            @Value("${discord.client-id}") String clientId,
            @Value("${discord.client-secret}") String clientSecret,
            @Value("${discord.redirect-uri}") String redirectUri
    ) {
        this.restClient = RestClient.create();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    public DiscordTokenResponse exchangeCode(String code, String requestRedirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();

        // 프론트가 authorize에 쓴 redirect_uri를 그대로 써야 Discord가 code를 받아준다.
        // (접속 IP마다 주소가 달라 고정 불가) 값이 없으면 설정의 기본값으로 폴백.
        String effectiveRedirectUri =
                (requestRedirectUri == null || requestRedirectUri.isBlank()) ? redirectUri : requestRedirectUri;

        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", effectiveRedirectUri);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        try {
            return restClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(DiscordTokenResponse.class);
        } catch (RestClientResponseException e) {
            throw new ResponseStatusException(
              HttpStatus.UNAUTHORIZED,
              "Discord 인증 코드가 유효하지 않습니다. 다시 로그인해주세요."
            );
        }
    }

    public DiscordUserResponse fetchUser(String discordAccessToken) {
        try {
            return restClient.get()
                    .uri(USER_URL)
                    .headers(headers ->
                            headers.setBearerAuth(discordAccessToken)
                    )
                    .retrieve()
                    .body(DiscordUserResponse.class);
        } catch (RestClientResponseException e) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Discord 사용자 정보를 조회할 수 없습니다. 다시 로그인해주세요."
            );
        }
    }
}
