package norimaets.appnotificationserver.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import norimaets.appnotificationserver.dto.DiscordDmRequest;
import norimaets.appnotificationserver.dto.DmResultStatus;
import norimaets.appnotificationserver.dto.PriceAlertNotificationEvent;
import norimaets.appnotificationserver.dto.UserNotificationCreateRequest;
import norimaets.appnotificationserver.dto.UserNotificationCreateStatus;
import norimaets.appnotificationserver.service.DiscordDmService;
import norimaets.appnotificationserver.service.UserNotificationService;
import norimaets.moduledomainmongo.document.UserNotificationType;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 배치 서버가 발행한 통합 알림 이벤트를 수신해
 *   1) 웹 알림 저장 (항상)
 *   2) Discord DM 발송 (event.discordUserId()가 있을 때만)
 * 을 순서대로 처리한다. 각 단계의 중복 방지는 UserNotificationService/DiscordDmService가
 * eventKey 기준으로 이미 처리하므로(REST 경로와 동일한 로직 재사용), 컨슈머는 순서만 조율한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PriceAlertNotificationConsumer {

    private final ObjectMapper objectMapper;
    private final UserNotificationService userNotificationService;
    private final DiscordDmService discordDmService;

    @KafkaListener(topics = "price-alert-notifications", groupId = "notification-service")
    public void consume(String message) {
        PriceAlertNotificationEvent event = parse(message);
        if (event == null) return;

        UserNotificationCreateStatus notificationStatus = userNotificationService.create(
                new UserNotificationCreateRequest(
                        event.eventKey(),
                        UserNotificationType.valueOf(event.notificationType()),
                        event.alertId(),
                        event.userId(),
                        event.gameId(),
                        event.gameName(),
                        event.targetPrice(),
                        event.currentPrice(),
                        event.discountPercent()
                )
        );
        log.info("웹 알림 저장 완료: eventKey={}, status={}", event.eventKey(), notificationStatus);

        if (event.discordUserId() == null || event.discordUserId().isBlank()) {
            return;
        }

        DmResultStatus dmStatus = discordDmService.send(
                new DiscordDmRequest(
                        event.eventKey(),
                        event.notificationType(),
                        event.alertId(),
                        event.userId(),
                        event.discordUserId(),
                        event.gameId(),
                        event.gameName(),
                        event.targetPrice(),
                        event.currentPrice(),
                        event.discountPercent()
                )
        );
        log.info("Discord DM 발송 처리 완료: eventKey={}, status={}", event.eventKey(), dmStatus);
    }

    private PriceAlertNotificationEvent parse(String message) {
        try {
            return objectMapper.readValue(message, PriceAlertNotificationEvent.class);
        } catch (Exception e) {
            log.error("가격 알림 이벤트 파싱 실패: {}", message, e);
            return null;
        }
    }
}
