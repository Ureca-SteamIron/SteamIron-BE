package norimaets.appbatchserver.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import norimaets.appbatchserver.client.DiscordNotificationClient;
import norimaets.appbatchserver.dto.notification.DiscordDmRequest;
import norimaets.appbatchserver.dto.notification.DiscordDmResponse;
import norimaets.appbatchserver.dto.notification.DiscordDmStatus;
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
                discordNotificationClient
        );
    }

    @Test
    @DisplayName("현재 가격이 목표가 이하이고 발송에 성공하면 알림 정보를 갱신한다")
    void reachedTargetAndSentUpdatesLastNotified() {
        givenSendableAlert();

        when(discordNotificationClient.sendDm(any(DiscordDmRequest.class)))
                .thenReturn(new DiscordDmResponse(DiscordDmStatus.SENT));

        service.process(game);

        verify(alert).updateLastNotified(
                eq(19_000),
                any(LocalDateTime.class)
        );

        ArgumentCaptor<DiscordDmRequest> requestCaptor =
                ArgumentCaptor.forClass(DiscordDmRequest.class);

        verify(discordNotificationClient)
                .sendDm(requestCaptor.capture());

        DiscordDmRequest request = requestCaptor.getValue();

        org.junit.jupiter.api.Assertions.assertEquals(
                "price-alert:123:19000",
                request.eventKey()
        );
    }

    @Test
    @DisplayName("현재 가격이 목표가보다 높으면 알림을 보내지 않는다")
    void priceAboveTargetDoesNotSend() {
        when(game.getId()).thenReturn(730L);
        when(game.getFinalPrice()).thenReturn(25_000);
        when(priceAlertRepository.findByGame_IdAndIsActiveTrue(730L))
                .thenReturn(List.of(alert));
        when(alert.getUser()).thenReturn(user);
        when(user.isDiscordNotificationEnabled()).thenReturn(true);
        when(alert.getTargetPrice()).thenReturn(20_000);

        service.process(game);

        verifyNoInteractions(discordNotificationClient);
        verify(alert, never()).updateLastNotified(
                anyInt(),
                any(LocalDateTime.class)
        );
    }

    @Test
    @DisplayName("사용자 전체 Discord 알림이 꺼져 있으면 발송하지 않는다")
    void globalNotificationOffDoesNotSend() {
        when(game.getId()).thenReturn(730L);
        when(game.getFinalPrice()).thenReturn(19_000);
        when(priceAlertRepository.findByGame_IdAndIsActiveTrue(730L))
                .thenReturn(List.of(alert));
        when(alert.getUser()).thenReturn(user);
        when(user.isDiscordNotificationEnabled()).thenReturn(false);

        service.process(game);

        verifyNoInteractions(discordNotificationClient);
        verify(alert, never()).updateLastNotified(
                anyInt(),
                any(LocalDateTime.class)
        );
    }

    @Test
    @DisplayName("현재 가격으로 이미 알림을 보냈다면 중복 발송하지 않는다")
    void sameLastNotifiedPriceDoesNotSendAgain() {
        when(game.getId()).thenReturn(730L);
        when(game.getFinalPrice()).thenReturn(19_000);
        when(priceAlertRepository.findByGame_IdAndIsActiveTrue(730L))
                .thenReturn(List.of(alert));
        when(alert.getUser()).thenReturn(user);
        when(user.isDiscordNotificationEnabled()).thenReturn(true);
        when(alert.getTargetPrice()).thenReturn(20_000);
        when(alert.getLastNotifiedPrice()).thenReturn(19_000);

        service.process(game);

        verifyNoInteractions(discordNotificationClient);
        verify(alert, never()).updateLastNotified(
                anyInt(),
                any(LocalDateTime.class)
        );
    }

    @Test
    @DisplayName("이미 발송된 알림이면 완료된 것으로 보고 알림 정보를 갱신한다")
    void alreadySentUpdatesLastNotified() {
        givenSendableAlert();

        when(discordNotificationClient.sendDm(any(DiscordDmRequest.class)))
                .thenReturn(
                        new DiscordDmResponse(
                                DiscordDmStatus.ALREADY_SENT
                        )
                );

        service.process(game);

        verify(discordNotificationClient)
                .sendDm(any(DiscordDmRequest.class));

        verify(alert).updateLastNotified(
                eq(19_000),
                any(LocalDateTime.class)
        );
    }


    @Test
    @DisplayName("Discord 발송에 실패하면 알림 정보를 갱신하지 않는다")
    void failedDeliveryDoesNotUpdateLastNotified() {
        givenSendableAlert();

        when(discordNotificationClient.sendDm(any(DiscordDmRequest.class)))
                .thenReturn(new DiscordDmResponse(DiscordDmStatus.FAILED));

        service.process(game);

        verify(discordNotificationClient)
                .sendDm(any(DiscordDmRequest.class));

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
        when(alert.getTargetPrice()).thenReturn(20_000);
        when(alert.getLastNotifiedPrice()).thenReturn(null);

        when(user.getId()).thenReturn(10L);
        when(user.getDiscordId()).thenReturn("123456789012345678");
        when(user.isDiscordNotificationEnabled()).thenReturn(true);
    }
}
