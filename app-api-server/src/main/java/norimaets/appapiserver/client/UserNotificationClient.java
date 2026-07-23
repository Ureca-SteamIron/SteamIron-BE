package norimaets.appapiserver.client;

import java.time.Duration;
import norimaets.appapiserver.client.dto.NotificationServerPageResponse;
import norimaets.appapiserver.dto.response.UnreadNotificationCountResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

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

    public NotificationServerPageResponse getNotifications(
            Long userId,
            int page,
            int size
    ) {
        NotificationServerPageResponse response = notificationWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/internal/v1/notifications")
                        .queryParam("userId", userId)
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build())
                .retrieve()
                .bodyToMono(NotificationServerPageResponse.class)
                .timeout(REQUEST_TIMEOUT)
                .block();

        if (response == null) {
            throw new IllegalStateException("알림 서버의 목록 응답이 없습니다.");
        }
        return response;
    }

    public UnreadNotificationCountResponse getUnreadCount(Long userId) {
        UnreadNotificationCountResponse response = notificationWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/internal/v1/notifications/unread-count")
                        .queryParam("userId", userId)
                        .build())
                .retrieve()
                .bodyToMono(UnreadNotificationCountResponse.class)
                .timeout(REQUEST_TIMEOUT)
                .block();

        if (response == null) {
            throw new IllegalStateException("알림 서버의 미확인 개수 응답이 없습니다.");
        }
        return response;
    }

    public void markAsRead(String notificationId, Long userId) {
        try {
            notificationWebClient.patch()
                    .uri(uriBuilder -> uriBuilder
                            .path("/internal/v1/notifications/{notificationId}/read")
                            .queryParam("userId", userId)
                            .build(notificationId))
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(REQUEST_TIMEOUT)
                    .block();
        } catch (WebClientResponseException.NotFound exception) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "알림을 찾을 수 없습니다."
            );
        }
    }
}
