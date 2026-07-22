package norimaets.appnotificationserver.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Discord 봇 REST API 클라이언트.
 * 개인 DM 발송은 2단계:
 *   ① POST /users/@me/channels  { recipient_id } → DM 채널 id
 *   ② POST /channels/{id}/messages  { content }  → 메시지 id
 * 봇 토큰은 "Authorization: Bot {token}" 헤더로 인증한다.
 */
@Component
public class DiscordApiClient {

    private static final String BASE_URL = "https://discord.com/api/v10";

    private final RestClient restClient;

    public DiscordApiClient(@Value("${discord.bot-token}") String botToken) {
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("Authorization", "Bot " + botToken)
                .build();
    }

    /** 유저와의 DM 채널을 열고 채널 id를 반환한다. (RestClientResponseException 발생 가능 — 호출측에서 처리) */
    public String openDmChannel(String discordUserId) {
        DiscordChannel channel = restClient.post()
                .uri("/users/@me/channels")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("recipient_id", discordUserId))
                .retrieve()
                .body(DiscordChannel.class);
        return channel.id();
    }

    /** DM 채널에 메시지를 보내고 메시지 id를 반환한다. */
    public String sendMessage(String channelId, String content) {
        DiscordMessage message = restClient.post()
                .uri("/channels/{channelId}/messages", channelId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("content", content))
                .retrieve()
                .body(DiscordMessage.class);
        return message.id();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DiscordChannel(String id) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record DiscordMessage(String id) {
    }
}
