package norimaets.appbatchserver.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import norimaets.appbatchserver.client.DiscordNotificationClient;
import norimaets.appbatchserver.client.UserNotificationClient;
import norimaets.appbatchserver.dto.notification.DiscordDmRequest;
import norimaets.appbatchserver.dto.notification.DiscordDmResponse;
import norimaets.appbatchserver.dto.notification.UserNotificationCreateRequest;
import norimaets.appbatchserver.dto.notification.UserNotificationCreateResponse;
import norimaets.moduledomainrdb.entity.Game;
import norimaets.moduledomainrdb.entity.PriceAlert;
import norimaets.moduledomainrdb.entity.User;
import norimaets.moduledomainrdb.repository.PriceAlertRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceAlertNotificationService {

    private final PriceAlertRepository priceAlertRepository;
    private final UserNotificationClient userNotificationClient;
    private final DiscordNotificationClient discordNotificationClient;

    @Transactional
    public void process(Game game, boolean discountStarted) {
        Integer currentPrice = game.getFinalPrice();

        if (currentPrice == null) {
            log.debug(
                    "현재 가격이 없어 알림 판정을 건너뜁니다: gameId={}",
                    game.getId()
            );
            return;
        }

        List<PriceAlert> alerts =
                priceAlertRepository.findByGame_IdAndIsActiveTrue(game.getId());

        for (PriceAlert alert : alerts) {
            processAlert(alert, game, currentPrice, discountStarted);
        }
    }

    private void processAlert(
            PriceAlert alert,
            Game game,
            Integer currentPrice,
            boolean discountStarted
    ) {
        User user = alert.getUser();
        LocalDateTime notifiedAt = LocalDateTime.now();

        if (discountStarted
                && alert.isDiscountStartEnabled()
                && !Objects.equals(alert.getLastDiscountStartNotifiedPrice(), currentPrice)
                && sendAlert(alert, game, currentPrice, user, "DISCOUNT_START", null)) {
            alert.updateDiscountStartLastNotified(currentPrice, notifiedAt);
        }

        Integer targetPrice = alert.getTargetPrice();
        if (alert.isTargetDiscountEnabled()
                && targetPrice != null
                && currentPrice <= targetPrice
                && !Objects.equals(alert.getLastNotifiedPrice(), currentPrice)
                && sendAlert(alert, game, currentPrice, user, "TARGET_PRICE", targetPrice)) {
            alert.updateLastNotified(currentPrice, notifiedAt);
        }
    }

    private boolean sendAlert(
            PriceAlert alert,
            Game game,
            Integer currentPrice,
            User user,
            String notificationType,
            Integer targetPrice
    ) {
        String eventKey = createEventKey(alert.getId(), notificationType, currentPrice);

        UserNotificationCreateRequest notificationRequest =
                new UserNotificationCreateRequest(
                        eventKey,
                        notificationType,
                        alert.getId(),
                        user.getId(),
                        game.getId(),
                        game.getName(),
                        targetPrice,
                        currentPrice,
                        game.getDiscountPercent()
                );

        UserNotificationCreateResponse notificationResponse =
                userNotificationClient.create(notificationRequest);

        if (notificationResponse == null || !notificationResponse.isCompleted()) {
            log.warn(
                    "웹 알림 저장 미완료: eventKey={}, status={}",
                    eventKey,
                    notificationResponse != null ? notificationResponse.status() : null
            );
            return false;
        }

        sendDiscordIfEnabled(
                user,
                alert,
                game,
                currentPrice,
                notificationType,
                targetPrice,
                eventKey
        );

        log.info(
                "웹 가격 알림 저장 완료: eventKey={}, type={}, status={}",
                eventKey,
                notificationType,
                notificationResponse.status()
        );
        return true;
    }

    private void sendDiscordIfEnabled(
            User user,
            PriceAlert alert,
            Game game,
            Integer currentPrice,
            String notificationType,
            Integer targetPrice,
            String eventKey
    ) {
        if (!user.isDiscordNotificationEnabled()) {
            return;
        }

        String discordUserId = user.getDiscordId();
        if (discordUserId == null || discordUserId.isBlank()) {
            log.warn(
                    "Discord 사용자 ID가 없어 DM을 보내지 않습니다: userId={}",
                    user.getId()
            );
            return;
        }

        DiscordDmRequest request = new DiscordDmRequest(
                eventKey,
                notificationType,
                alert.getId(),
                user.getId(),
                discordUserId,
                game.getId(),
                game.getName(),
                targetPrice,
                currentPrice,
                game.getDiscountPercent()
        );

        DiscordDmResponse response = discordNotificationClient.sendDm(request);

        if (response != null && response.isCompleted()) {
            log.info(
                    "Discord 가격 알림 발송 완료: eventKey={}, type={}, status={}",
                    eventKey,
                    notificationType,
                    response.status()
            );
            return;
        }

        log.warn(
                "Discord 가격 알림 발송 미완료: eventKey={}, status={}",
                eventKey,
                response != null ? response.status() : null
        );
    }

    private String createEventKey(
            Long alertId,
            String notificationType,
            Integer currentPrice
    ) {
        return "price-alert:" + alertId + ":" + notificationType.toLowerCase() + ":" + currentPrice;
    }
}
