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
import norimaets.appbatchserver.client.DiscordNotificationClient;
import norimaets.appbatchserver.client.UserNotificationClient;
import norimaets.appbatchserver.dto.notification.DiscordDmRequest;
import norimaets.appbatchserver.dto.notification.DiscordDmResponse;
import norimaets.appbatchserver.dto.notification.DiscordDmStatus;
import norimaets.appbatchserver.dto.notification.UserNotificationCreateRequest;
import norimaets.appbatchserver.dto.notification.UserNotificationCreateResponse;
import norimaets.appbatchserver.dto.notification.UserNotificationCreateStatus;
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
    private UserNotificationClient userNotificationClient;

    @Mock
    private DiscordNotificationClient discordNotificationClient;

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
                userNotificationClient,
                discordNotificationClient
        );
    }

    @Test
    @DisplayName("할인 시작과 목표 가격 조건을 만족하면 웹 알림 두 건을 만들고 Discord로도 전송한다")
    void createsDiscountStartAndTargetPriceNotifications() {
        givenSendableAlert();
        when(alert.isDiscountStartEnabled()).thenReturn(true);
        when(user.isDiscordNotificationEnabled()).thenReturn(true);
        when(user.getDiscordId()).thenReturn("123456789012345678");
        when(userNotificationClient.create(any(UserNotificationCreateRequest.class)))
                .thenReturn(completed(UserNotificationCreateStatus.CREATED));
        when(discordNotificationClient.sendDm(any(DiscordDmRequest.class)))
                .thenReturn(new DiscordDmResponse(DiscordDmStatus.SENT));

        service.process(game, true);

        ArgumentCaptor<UserNotificationCreateRequest> requestCaptor =
                ArgumentCaptor.forClass(UserNotificationCreateRequest.class);
        verify(userNotificationClient, times(2)).create(requestCaptor.capture());

        assertThat(requestCaptor.getAllValues())
                .extracting(UserNotificationCreateRequest::notificationType)
                .containsExactly("DISCOUNT_START", "TARGET_PRICE");
        assertThat(requestCaptor.getAllValues())
                .extracting(UserNotificationCreateRequest::eventKey)
                .containsExactly(
                        "price-alert:123:discount_start:19000",
                        "price-alert:123:target_price:19000"
                );

        verify(discordNotificationClient, times(2))
                .sendDm(any(DiscordDmRequest.class));
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
    @DisplayName("Discord가 꺼져 있어도 웹 알림은 생성하고 완료 가격을 갱신한다")
    void discordOffStillCreatesWebNotification() {
        givenSendableAlert();
        when(user.isDiscordNotificationEnabled()).thenReturn(false);
        when(userNotificationClient.create(any(UserNotificationCreateRequest.class)))
                .thenReturn(completed(UserNotificationCreateStatus.CREATED));

        service.process(game, false);

        verify(userNotificationClient).create(any(UserNotificationCreateRequest.class));
        verifyNoInteractions(discordNotificationClient);
        verify(alert).updateLastNotified(
                eq(19_000),
                any(LocalDateTime.class)
        );
    }

    @Test
    @DisplayName("이미 저장된 웹 알림도 완료로 보고 가격을 갱신한다")
    void alreadyExistingWebNotificationUpdatesLastNotified() {
        givenSendableAlert();
        when(user.isDiscordNotificationEnabled()).thenReturn(false);
        when(userNotificationClient.create(any(UserNotificationCreateRequest.class)))
                .thenReturn(completed(UserNotificationCreateStatus.ALREADY_EXISTS));

        service.process(game, false);

        verify(alert).updateLastNotified(
                eq(19_000),
                any(LocalDateTime.class)
        );
    }

    @Test
    @DisplayName("웹 알림 저장에 실패하면 완료 가격을 갱신하지 않고 Discord도 호출하지 않는다")
    void failedWebNotificationDoesNotUpdateLastNotified() {
        givenSendableAlert();
        when(userNotificationClient.create(any(UserNotificationCreateRequest.class)))
                .thenReturn(completed(UserNotificationCreateStatus.FAILED));

        service.process(game, false);

        verifyNoInteractions(discordNotificationClient);
        verify(alert, never()).updateLastNotified(
                anyInt(),
                any(LocalDateTime.class)
        );
    }

    @Test
    @DisplayName("Discord 전송이 실패해도 웹 알림이 저장됐으면 완료 가격을 갱신한다")
    void failedDiscordStillUpdatesLastNotified() {
        givenSendableAlert();
        when(user.isDiscordNotificationEnabled()).thenReturn(true);
        when(user.getDiscordId()).thenReturn("123456789012345678");
        when(userNotificationClient.create(any(UserNotificationCreateRequest.class)))
                .thenReturn(completed(UserNotificationCreateStatus.CREATED));
        when(discordNotificationClient.sendDm(any(DiscordDmRequest.class)))
                .thenReturn(new DiscordDmResponse(DiscordDmStatus.FAILED));

        service.process(game, false);

        verify(discordNotificationClient).sendDm(any(DiscordDmRequest.class));
        verify(alert).updateLastNotified(
                eq(19_000),
                any(LocalDateTime.class)
        );
    }

    @Test
    @DisplayName("현재 가격이 목표 가격보다 높으면 어떤 알림도 생성하지 않는다")
    void priceAboveTargetDoesNotCreateNotification() {
        when(game.getId()).thenReturn(730L);
        when(game.getFinalPrice()).thenReturn(25_000);
        when(priceAlertRepository.findByGame_IdAndIsActiveTrue(730L))
                .thenReturn(List.of(alert));
        when(alert.getUser()).thenReturn(user);
        when(alert.isTargetDiscountEnabled()).thenReturn(true);
        when(alert.getTargetPrice()).thenReturn(20_000);

        service.process(game, false);

        verifyNoInteractions(userNotificationClient, discordNotificationClient);
        verify(alert, never()).updateLastNotified(
                anyInt(),
                any(LocalDateTime.class)
        );
    }

    @Test
    @DisplayName("현재 가격으로 이미 알림을 완료했다면 다시 생성하지 않는다")
    void sameLastNotifiedPriceDoesNotCreateNotification() {
        when(game.getId()).thenReturn(730L);
        when(game.getFinalPrice()).thenReturn(19_000);
        when(priceAlertRepository.findByGame_IdAndIsActiveTrue(730L))
                .thenReturn(List.of(alert));
        when(alert.getUser()).thenReturn(user);
        when(alert.isTargetDiscountEnabled()).thenReturn(true);
        when(alert.getTargetPrice()).thenReturn(20_000);
        when(alert.getLastNotifiedPrice()).thenReturn(19_000);

        service.process(game, false);

        verifyNoInteractions(userNotificationClient, discordNotificationClient);
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

    private UserNotificationCreateResponse completed(
            UserNotificationCreateStatus status
    ) {
        return new UserNotificationCreateResponse(status);
    }
}
