package norimaets.appbatchserver.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import norimaets.appbatchserver.dto.notification.DiscordDmRequest;
import norimaets.appbatchserver.dto.notification.DiscordDmResponse;
import norimaets.appbatchserver.dto.notification.DiscordDmStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * 배치 서버 → 알림 서버 Discord DM 요청 발행.
 * REST 동기 호출 대신 카프카에 발행하고, 실제 DM 발송(성공/실패/중복)은 알림 서버가 비동기로 처리한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DiscordNotificationClient {

    private static final String TOPIC = "discord-dm-events";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public DiscordDmResponse sendDm(DiscordDmRequest request) {
        try {
            String payload = objectMapper.writeValueAsString(request);
            kafkaTemplate.send(TOPIC, request.eventKey(), payload);
            return new DiscordDmResponse(DiscordDmStatus.QUEUED);
        } catch (JsonProcessingException e) {
            log.warn("Discord DM 이벤트 발행 실패: eventKey={}, message={}", request.eventKey(), e.getMessage());
            return new DiscordDmResponse(DiscordDmStatus.FAILED);
        }
    }
}
