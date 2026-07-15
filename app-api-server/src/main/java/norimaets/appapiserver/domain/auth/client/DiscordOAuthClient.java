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
        // TODO: code를 Discord access_token으로 교환하기 (흐름 ④)
        //  1) form-urlencoded 본문 만들기 (MultiValueMap)
        //     - grant_type=authorization_code, code, redirect_uri, client_id, client_secret
        //     - ※ OAuth2 표준상 토큰 요청은 JSON이 아니라 form 형식이어야 함
        //  2) restClient.post() 로 TOKEN_URL 에 POST
        //     - contentType(APPLICATION_FORM_URLENCODED), body(form)
        //  3) 응답을 DiscordTokenResponse 로 변환해 반환
        //  4) RestClientResponseException(4xx) 발생 시 → 401 ResponseStatusException 으로 변환
        //     ("Discord 인증 코드가 유효하지 않습니다. 다시 로그인해주세요.")
        return null;
    }

    public DiscordUserResponse fetchUser(String discordAccessToken) {
        // TODO: access_token으로 Discord 유저 정보 조회하기 (흐름 ⑤)
        //  1) restClient.get() 으로 USER_URL 에 GET
        //  2) 헤더에 "Authorization: Bearer " + discordAccessToken 추가
        //  3) 응답을 DiscordUserResponse 로 변환해 반환
        //  4) 실패(4xx) 시 → 401 ResponseStatusException 으로 변환
        return null;
    }
}
