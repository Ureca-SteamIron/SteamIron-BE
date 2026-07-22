package norimaets.appbatchserver.client;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import norimaets.appbatchserver.dto.notification.DiscordDmRequest;
import norimaets.appbatchserver.dto.notification.DiscordDmResponse;
import norimaets.appbatchserver.dto.notification.DiscordDmStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class DiscordNotificationClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final WebClient notificationWebClient;

    public DiscordNotificationClient(
            @Qualifier("notificationWebClient")
            WebClient notificationWebClient
    ) {
        this.notificationWebClient = notificationWebClient;
    }

    public DiscordDmResponse sendDm(DiscordDmRequest request) {
        try {
            DiscordDmResponse response = notificationWebClient.post()
                    .uri("/internal/v1/discord/dms")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(DiscordDmResponse.class)
                    .timeout(REQUEST_TIMEOUT)
                    .block();

            if (response == null) {
                log.warn(
                        "알림 서버 응답 없음: eventKey={}",
                        request.eventKey()
                );

                return new DiscordDmResponse(DiscordDmStatus.FAILED);
            }

            return response;
        } catch (RuntimeException exception) {
            log.warn(
                    "알림 서버 호출 실패: eventKey={}, message={}",
                    request.eventKey(),
                    exception.getMessage()
            );

            return new DiscordDmResponse(DiscordDmStatus.FAILED);
        }
    }
}