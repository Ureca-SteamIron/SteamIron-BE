package norimaets.appbatchserver.client;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import norimaets.appbatchserver.dto.notification.UserNotificationCreateRequest;
import norimaets.appbatchserver.dto.notification.UserNotificationCreateResponse;
import norimaets.appbatchserver.dto.notification.UserNotificationCreateStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class UserNotificationClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final WebClient notificationWebClient;

    public UserNotificationClient(
            @Qualifier("notificationWebClient")
            WebClient notificationWebClient
    ) {
        this.notificationWebClient = notificationWebClient;
    }

    public UserNotificationCreateResponse create(
            UserNotificationCreateRequest request
    ) {
        try {
            UserNotificationCreateResponse response = notificationWebClient.post()
                    .uri("/internal/v1/notifications")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(UserNotificationCreateResponse.class)
                    .timeout(REQUEST_TIMEOUT)
                    .block();

            if (response == null) {
                log.warn(
                        "알림 서버의 웹 알림 생성 응답이 없습니다: eventKey={}",
                        request.eventKey()
                );

                return failedResponse();
            }

            return response;
        } catch (RuntimeException exception) {
            log.warn(
                    "알림 서버의 웹 알림 생성 호출에 실패했습니다: eventKey={}, message={}",
                    request.eventKey(),
                    exception.getMessage()
            );

            return failedResponse();
        }
    }

    private UserNotificationCreateResponse failedResponse() {
        return new UserNotificationCreateResponse(
                UserNotificationCreateStatus.FAILED
        );
    }
}
