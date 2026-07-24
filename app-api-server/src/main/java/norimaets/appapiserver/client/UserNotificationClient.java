package norimaets.appapiserver.client;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import norimaets.appapiserver.client.dto.NotificationServerPageResponse;
import norimaets.appapiserver.dto.response.UnreadNotificationCountResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

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

    // 회원 탈퇴 시 알림 서버의 유저 알림 데이터 삭제 요청(베스트 에포트).
    // 알림 서버가 죽어있거나 실패해도 탈퇴 자체는 진행되어야 하므로 예외를 삼키고 로그만 남긴다.
    // (추후 Kafka 전환 시 이 지점을 이벤트 발행으로 교체)
    public void deleteAllByUserId(Long userId) {
        try {
            notificationWebClient.delete()
                    .uri(uriBuilder -> uriBuilder
                            .path("/internal/v1/notifications")
                            .queryParam("userId", userId)
                            .build())
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(REQUEST_TIMEOUT)
                    .block();
        } catch (Exception exception) {
            log.warn(
                    "알림 서버 유저 데이터 삭제 실패 (탈퇴는 계속 진행): userId={}, cause={}",
                    userId,
                    exception.getMessage()
            );
        }
    }
}
