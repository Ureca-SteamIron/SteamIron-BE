package norimaets.appbatchserver.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import norimaets.appbatchserver.dto.notification.PriceAlertNotificationEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 가격 알림 판정 결과를 카프카로 발행한다.
 * 배치가 가격 diff → 알림 조건 판정까지 끝낸 결과물을 알림 서버에 전달하는 유일한 통로.
 * 실제 웹 알림 저장/Discord DM 발송은 알림 서버 컨슈머가 비동기로 처리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PriceAlertNotificationProducer {

    private static final String TOPIC = "price-alert-notifications";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * @return 발행 성공 여부. false면 알림 자체가 큐에 들어가지 못한 것이므로,
     *         호출부(PriceAlertNotificationService)는 이 경우 lastNotifiedPrice를 갱신하면 안 된다
     *         (안 그러면 이 알림은 영영 재시도되지 않는다).
     */
    public boolean publish(PriceAlertNotificationEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            // 키를 eventKey로 둬서, 같은 alert의 재시도성 이벤트가 같은 파티션에 순서대로 쌓이게 한다.
            kafkaTemplate.send(TOPIC, event.eventKey(), payload);
            log.info("가격 알림 이벤트 발행: eventKey={}, type={}", event.eventKey(), event.notificationType());
            return true;
        } catch (JsonProcessingException e) {
            log.error("가격 알림 이벤트 직렬화 실패: eventKey={}", event.eventKey(), e);
            return false;
        }
    }
}