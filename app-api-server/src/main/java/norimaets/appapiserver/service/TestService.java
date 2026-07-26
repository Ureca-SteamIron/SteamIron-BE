package norimaets.appapiserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import norimaets.appapiserver.dto.notification.PriceAlertNotificationEvent;
import norimaets.appapiserver.dto.request.TestPriceAlertRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * 배치의 스케줄 실행을 기다리지 않고 가격 알림 카프카 이벤트를 즉시 발행해보기 위한 테스트 전용 서비스.
 * app-batch-server의 PriceAlertNotificationProducer와 동일한 토픽·페이로드 스키마를 사용한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TestService {

    private static final String TOPIC = "price-alert-notifications";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public boolean publishTestPriceAlert(TestPriceAlertRequest request) {
        String eventKey = createEventKey(request);

        PriceAlertNotificationEvent event = new PriceAlertNotificationEvent(
                eventKey,
                request.notificationType(),
                request.alertId(),
                request.userId(),
                request.discordUserId(),
                request.gameId(),
                request.gameName(),
                request.targetPrice(),
                request.currentPrice(),
                request.discountPercent()
        );

        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(TOPIC, eventKey, payload);
            log.info("[TEST] 가격 알림 이벤트 발행: eventKey={}, type={}", eventKey, event.notificationType());
            return true;
        } catch (JsonProcessingException e) {
            log.error("[TEST] 가격 알림 이벤트 직렬화 실패: eventKey={}", eventKey, e);
            return false;
        }
    }

    private String createEventKey(TestPriceAlertRequest request) {
        long alertId = request.alertId() != null ? request.alertId() : 0L;
        return "test:" + alertId + ":" + request.notificationType() + ":" + request.currentPrice();
    }
}
