package norimaets.appapiserver.domain.auth.client;

import norimaets.appapiserver.domain.auth.dto.DiscordTokenResponse;
import norimaets.appapiserver.domain.auth.dto.DiscordUserResponse;
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

    public DiscordTokenResponse exchangeCode(String code) {
        // OAuth2 표준상 토큰 요청은 JSON이 아니라 form-urlencoded 형식이어야 한다
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("redirect_uri", redirectUri); // 인증 요청 때 쓴 값과 일치해야 함 (검증용)
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
            // code가 만료됐거나, 이미 사용됐거나, redirect_uri가 불일치하는 경우
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Discord 인증 코드가 유효하지 않습니다. 다시 로그인해주세요.");
        }
    }

    public DiscordUserResponse fetchUser(String discordAccessToken) {
        try {
            return restClient.get()
                    .uri(USER_URL)
                    .header("Authorization", "Bearer " + discordAccessToken)
                    .retrieve()
                    .body(DiscordUserResponse.class);
        } catch (RestClientResponseException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Discord 유저 정보 조회에 실패했습니다.");
        }
    }
}
