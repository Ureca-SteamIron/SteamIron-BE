package norimaets.appnotificationserver.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import norimaets.appnotificationserver.dto.DiscordDmRequest;
import norimaets.appnotificationserver.dto.DmResultStatus;
import norimaets.appnotificationserver.dto.PriceAlertNotificationEvent;
import norimaets.appnotificationserver.dto.UserNotificationCreateRequest;
import norimaets.appnotificationserver.dto.UserNotificationCreateStatus;
import norimaets.appnotificationserver.service.DiscordDmService;
import norimaets.appnotificationserver.service.UserNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PriceAlertNotificationConsumerTest {

    @Mock
    private UserNotificationService userNotificationService;

    @Mock
    private DiscordDmService discordDmService;

    private PriceAlertNotificationConsumer consumer;

    @BeforeEach
    void setUp() {
        // 실제 JSON 파싱까지 검증해야 하므로 ObjectMapper는 mock이 아니라 진짜 인스턴스를 사용
        consumer = new PriceAlertNotificationConsumer(new ObjectMapper(), userNotificationService, discordDmService);
    }

    @Test
    @DisplayName("discordUserId가 있으면 웹 알림 저장 후 Discord DM도 발송한다")
    void consumesEventWithDiscordUserId() throws Exception {
        PriceAlertNotificationEvent event = new PriceAlertNotificationEvent(
                "price-alert:123:target_price:15000",
                "TARGET_PRICE",
                123L,
                10L,
                "963758346303832084",
                2561650L,
                "GUNRUN",
                20000,
                15000,
                25
        );
        when(userNotificationService.create(any(UserNotificationCreateRequest.class)))
                .thenReturn(UserNotificationCreateStatus.CREATED);
        when(discordDmService.send(any(DiscordDmRequest.class)))
                .thenReturn(DmResultStatus.SENT);

        consumer.consume(toJson(event));

        ArgumentCaptor<UserNotificationCreateRequest> notificationCaptor =
                ArgumentCaptor.forClass(UserNotificationCreateRequest.class);
        verify(userNotificationService).create(notificationCaptor.capture());
        UserNotificationCreateRequest notificationRequest = notificationCaptor.getValue();
        assertThat(notificationRequest.eventKey())
                .isEqualTo("price-alert:123:target_price:15000");
        assertThat(notificationRequest.gameId()).isEqualTo(2561650L);

        ArgumentCaptor<DiscordDmRequest> dmCaptor = ArgumentCaptor.forClass(DiscordDmRequest.class);
        verify(discordDmService).send(dmCaptor.capture());
        assertThat(dmCaptor.getValue().discordUserId())
                .isEqualTo("963758346303832084");
    }

    @Test
    @DisplayName("discordUserId가 없으면 웹 알림만 저장하고 Discord DM은 보내지 않는다")
    void consumesEventWithoutDiscordUserId() throws Exception {
        PriceAlertNotificationEvent event = new PriceAlertNotificationEvent(
                "price-alert:123:target_price:15000",
                "TARGET_PRICE",
                123L,
                10L,
                null,
                2561650L,
                "GUNRUN",
                20000,
                15000,
                25
        );
        when(userNotificationService.create(any(UserNotificationCreateRequest.class)))
                .thenReturn(UserNotificationCreateStatus.CREATED);

        consumer.consume(toJson(event));

        verify(userNotificationService, times(1)).create(any(UserNotificationCreateRequest.class));
        verify(discordDmService, never()).send(any(DiscordDmRequest.class));
    }

    @Test
    @DisplayName("JSON 파싱에 실패하면 예외 없이 조용히 무시한다")
    void ignoresMalformedMessage() {
        consumer.consume("이건 JSON이 아님");

        verify(userNotificationService, never()).create(any(UserNotificationCreateRequest.class));
        verify(discordDmService, never()).send(any(DiscordDmRequest.class));
    }

    private String toJson(PriceAlertNotificationEvent event) throws Exception {
        return new ObjectMapper().writeValueAsString(event);
    }
}
