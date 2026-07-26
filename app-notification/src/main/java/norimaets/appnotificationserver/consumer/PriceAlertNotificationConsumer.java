package norimaets.appnotificationserver.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
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
 * 배치 서버가 price-alert-notifications 토픽에 발행한 이벤트를 받아
 * "웹 알림 저장"과 "Discord DM 발송(선택)"을 모두 처리한다.
 * (PriceAlertNotificationProducer 문서에 적힌 원래 설계대로 — 이벤트 하나로 두 처리를 완결)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PriceAlertNotificationConsumer {

    private final ObjectMapper objectMapper;
    private final UserNotificationService userNotificationService;
    private final DiscordDmService discordDmService;

    @KafkaListener(topics = "price-alert-notifications")
    public void onMessage(String payload) {
        PriceAlertNotificationEvent event;
        try {
            event = objectMapper.readValue(payload, PriceAlertNotificationEvent.class);
        } catch (JsonProcessingException e) {
            log.error("가격 알림 이벤트 역직렬화 실패: payload={}", payload, e);
            return;
        }

        saveWebNotification(event);

        if (event.discordUserId() != null && !event.discordUserId().isBlank()) {
            sendDiscordDm(event);
        }
    }

    private void saveWebNotification(PriceAlertNotificationEvent event) {
        UserNotificationCreateRequest request = new UserNotificationCreateRequest(
                event.eventKey(),
                UserNotificationType.valueOf(event.notificationType()),
                event.alertId(),
                event.userId(),
                event.gameId(),
                event.gameName(),
                event.targetPrice(),
                event.currentPrice(),
                event.discountPercent()
        );

        UserNotificationCreateStatus status = userNotificationService.create(request);
        log.info("웹 알림 저장 결과: eventKey={}, status={}", event.eventKey(), status);
    }

    private void sendDiscordDm(PriceAlertNotificationEvent event) {
        DiscordDmRequest request = new DiscordDmRequest(
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
        );

        DmResultStatus status = discordDmService.send(request);
        log.info("Discord DM 발송 결과: eventKey={}, status={}", event.eventKey(), status);
    }
}
