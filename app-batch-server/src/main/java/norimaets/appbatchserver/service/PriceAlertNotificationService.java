package norimaets.appbatchserver.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import norimaets.appbatchserver.client.DiscordNotificationClient;
import norimaets.appbatchserver.dto.notification.DiscordDmRequest;
import norimaets.appbatchserver.dto.notification.DiscordDmResponse;
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

        if (!user.isDiscordNotificationEnabled()) {
            return;
        }

        String discordUserId = user.getDiscordId();

        if (discordUserId == null || discordUserId.isBlank()) {
            log.warn(
                    "Discord 사용자 ID가 없어 알림을 보낼 수 없습니다: userId={}",
                    user.getId()
            );
            return;
        }

        LocalDateTime notifiedAt = LocalDateTime.now();

        if (discountStarted
                && alert.isDiscountStartEnabled()
                && !Objects.equals(alert.getLastDiscountStartNotifiedPrice(), currentPrice)
                && sendAlert(alert, game, currentPrice, discordUserId, "DISCOUNT_START", null)) {
            alert.updateDiscountStartLastNotified(currentPrice, notifiedAt);
        }

        Integer targetPrice = alert.getTargetPrice();
        if (alert.isTargetDiscountEnabled()
                && targetPrice != null
                && currentPrice <= targetPrice
                && !Objects.equals(alert.getLastNotifiedPrice(), currentPrice)
                && sendAlert(alert, game, currentPrice, discordUserId, "TARGET_PRICE", targetPrice)) {
            alert.updateLastNotified(currentPrice, notifiedAt);
        }
    }

    private boolean sendAlert(
            PriceAlert alert,
            Game game,
            Integer currentPrice,
            String discordUserId,
            String notificationType,
            Integer targetPrice
    ) {
        String eventKey = createEventKey(alert.getId(), notificationType, currentPrice);

        DiscordDmRequest request = new DiscordDmRequest(
                eventKey,
                notificationType,
                alert.getId(),
                alert.getUser().getId(),
                discordUserId,
                game.getId(),
                game.getName(),
                targetPrice,
                currentPrice,
                game.getDiscountPercent()
        );
        // 알림서버 호출
        DiscordDmResponse response =
                discordNotificationClient.sendDm(request);

        if (response != null && response.isCompleted()) {
            log.info(
                    "가격 알림 발송 완료: eventKey={}, type={}, status={}",
                    eventKey,
                    notificationType,
                    response.status()
            );
            return true;
        }

        log.warn(
                "가격 알림 발송 미완료: eventKey={}, status={}",
                eventKey,
                response != null ? response.status() : null
        );
        return false;
    }

    private String createEventKey(
            Long alertId,
            String notificationType,
            Integer currentPrice
    ) {
        return "price-alert:" + alertId + ":" + notificationType.toLowerCase() + ":" + currentPrice;
    }
}
