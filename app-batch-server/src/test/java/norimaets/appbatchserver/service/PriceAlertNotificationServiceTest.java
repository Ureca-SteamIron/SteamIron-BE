package norimaets.appbatchserver.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import norimaets.appbatchserver.dto.notification.PriceAlertNotificationEvent;
import norimaets.appbatchserver.producer.PriceAlertNotificationProducer;
import norimaets.moduledomainrdb.entity.Game;
import norimaets.moduledomainrdb.entity.PriceAlert;
import norimaets.moduledomainrdb.entity.User;
import norimaets.moduledomainrdb.repository.PriceAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PriceAlertNotificationServiceTest {

    @Mock
    private PriceAlertRepository priceAlertRepository;

    @Mock
    private PriceAlertNotificationProducer notificationProducer;

    @Mock
    private Game game;

    @Mock
    private PriceAlert alert;

    @Mock
    private User user;

    private PriceAlertNotificationService service;

    @BeforeEach
    void setUp() {
        service = new PriceAlertNotificationService(
                priceAlertRepository,
                notificationProducer
        );
    }

    @Test
    @DisplayName("할인 시작과 목표 가격 조건을 만족하면 이벤트 두 건을 발행하고, Discord ID를 포함시킨다")
    void publishesDiscountStartAndTargetPriceEvents() {
        givenSendableAlert();
        when(alert.isDiscountStartEnabled()).thenReturn(true);
        when(user.isDiscordNotificationEnabled()).thenReturn(true);
        when(user.getDiscordId()).thenReturn("123456789012345678");
        when(notificationProducer.publish(any(PriceAlertNotificationEvent.class))).thenReturn(true);

        service.process(game, true);

        ArgumentCaptor<PriceAlertNotificationEvent> captor =
                ArgumentCaptor.forClass(PriceAlertNotificationEvent.class);
        verify(notificationProducer, times(2)).publish(captor.capture());

        assertThat(captor.getAllValues())
                .extracting(PriceAlertNotificationEvent::notificationType)
                .containsExactly("DISCOUNT_START", "TARGET_PRICE");
        assertThat(captor.getAllValues())
                .extracting(PriceAlertNotificationEvent::eventKey)
                .containsExactly(
                        "price-alert:123:discount_start:19000",
                        "price-alert:123:target_price:19000"
                );
        assertThat(captor.getAllValues())
                .extracting(PriceAlertNotificationEvent::discordUserId)
                .containsExactly("123456789012345678", "123456789012345678");

        verify(alert).updateDiscountStartLastNotified(
                eq(19_000),
                any(LocalDateTime.class)
        );
        verify(alert).updateLastNotified(
                eq(19_000),
                any(LocalDateTime.class)
        );
    }

    @Test
    @DisplayName("Discord가 꺼져 있으면 discordUserId 없이 이벤트를 발행하고 완료 가격을 갱신한다")
    void discordOffPublishesEventWithoutDiscordUserId() {
        givenSendableAlert();
        when(user.isDiscordNotificationEnabled()).thenReturn(false);
        when(notificationProducer.publish(any(PriceAlertNotificationEvent.class))).thenReturn(true);

        service.process(game, false);

        ArgumentCaptor<PriceAlertNotificationEvent> captor =
                ArgumentCaptor.forClass(PriceAlertNotificationEvent.class);
        verify(notificationProducer).publish(captor.capture());
        assertThat(captor.getValue().discordUserId()).isNull();

        verify(alert).updateLastNotified(
                eq(19_000),
                any(LocalDateTime.class)
        );
    }

    @Test
    @DisplayName("이벤트 발행에 실패하면 완료 가격을 갱신하지 않는다 (재시도 가능하게)")
    void failedPublishDoesNotUpdateLastNotified() {
        givenSendableAlert();
        when(user.isDiscordNotificationEnabled()).thenReturn(false);
        when(notificationProducer.publish(any(PriceAlertNotificationEvent.class))).thenReturn(false);

        service.process(game, false);

        verify(alert, never()).updateLastNotified(
                anyInt(),
                any(LocalDateTime.class)
        );
    }

    @Test
    @DisplayName("현재 가격이 목표 가격보다 높으면 어떤 이벤트도 발행하지 않는다")
    void priceAboveTargetDoesNotPublish() {
        when(game.getId()).thenReturn(730L);
        when(game.getFinalPrice()).thenReturn(25_000);
        when(priceAlertRepository.findByGame_IdAndIsActiveTrue(730L))
                .thenReturn(List.of(alert));
        when(alert.getUser()).thenReturn(user);
        when(alert.isTargetDiscountEnabled()).thenReturn(true);
        when(alert.getTargetPrice()).thenReturn(20_000);

        service.process(game, false);

        verifyNoInteractions(notificationProducer);
        verify(alert, never()).updateLastNotified(
                anyInt(),
                any(LocalDateTime.class)
        );
    }

    @Test
    @DisplayName("현재 가격으로 이미 알림을 완료했다면 다시 발행하지 않는다")
    void sameLastNotifiedPriceDoesNotPublish() {
        when(game.getId()).thenReturn(730L);
        when(game.getFinalPrice()).thenReturn(19_000);
        when(priceAlertRepository.findByGame_IdAndIsActiveTrue(730L))
                .thenReturn(List.of(alert));
        when(alert.getUser()).thenReturn(user);
        when(alert.isTargetDiscountEnabled()).thenReturn(true);
        when(alert.getTargetPrice()).thenReturn(20_000);
        when(alert.getLastNotifiedPrice()).thenReturn(19_000);

        service.process(game, false);

        verifyNoInteractions(notificationProducer);
        verify(alert, never()).updateLastNotified(
                anyInt(),
                any(LocalDateTime.class)
        );
    }

    private void givenSendableAlert() {
        when(game.getId()).thenReturn(730L);
        when(game.getName()).thenReturn("테스트 게임");
        when(game.getFinalPrice()).thenReturn(19_000);
        when(game.getDiscountPercent()).thenReturn(50);

        when(priceAlertRepository.findByGame_IdAndIsActiveTrue(730L))
                .thenReturn(List.of(alert));

        when(alert.getId()).thenReturn(123L);
        when(alert.getUser()).thenReturn(user);
        when(alert.isTargetDiscountEnabled()).thenReturn(true);
        when(alert.getTargetPrice()).thenReturn(20_000);

        when(user.getId()).thenReturn(10L);
    }
}
