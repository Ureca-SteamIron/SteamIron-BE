package norimaets.appbatchserver.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import norimaets.appbatchserver.dto.notification.PriceAlertNotificationEvent;
import norimaets.appbatchserver.producer.PriceAlertNotificationProducer;
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
    private final PriceAlertNotificationProducer notificationProducer;

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
                && publishAlert(alert, game, currentPrice, user, "DISCOUNT_START", null)) {
            alert.updateDiscountStartLastNotified(currentPrice, notifiedAt);
        }

        Integer targetPrice = alert.getTargetPrice();
        if (alert.isTargetDiscountEnabled()
                && targetPrice != null
                && currentPrice <= targetPrice
                && !Objects.equals(alert.getLastNotifiedPrice(), currentPrice)
                && publishAlert(alert, game, currentPrice, user, "TARGET_PRICE", targetPrice)) {
            alert.updateLastNotified(currentPrice, notifiedAt);
        }
    }

    /**
     * 카프카에 알림 이벤트를 발행한다.
     * 반환값은 "발행 성공 여부"이지 "실제 발송 성공 여부"가 아니다 — 웹 알림 저장/Discord DM은
     * 알림 서버 컨슈머가 비동기로 처리하므로, 여기서는 브로커에 들어갔는지만 확인할 수 있다.
     * (호출부는 이 반환값으로 lastNotifiedPrice 갱신 여부를 결정 — "낙관적 갱신" 정책)
     */
    private boolean publishAlert(
            PriceAlert alert,
            Game game,
            Integer currentPrice,
            User user,
            String notificationType,
            Integer targetPrice
    ) {
        String eventKey = createEventKey(alert.getId(), notificationType, currentPrice);

        // Discord 발송 여부 판단은 배치 서버(Postgres 접근 가능)에서 미리 끝내둔다.
        // null이면 알림 서버는 웹 알림만 저장하고 Discord는 건너뛴다.
        String discordUserId = resolveDiscordUserId(user);

        PriceAlertNotificationEvent event = new PriceAlertNotificationEvent(
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

        boolean published = notificationProducer.publish(event);
        if (!published) {
            log.warn("가격 알림 이벤트 발행 실패: eventKey={}, type={}", eventKey, notificationType);
        }
        return published;
    }

    private String resolveDiscordUserId(User user) {
        if (!user.isDiscordNotificationEnabled()) {
            return null;
        }
        String discordUserId = user.getDiscordId();
        if (discordUserId == null || discordUserId.isBlank()) {
            log.warn("Discord 알림이 켜져 있지만 discordId가 없습니다: userId={}", user.getId());
            return null;
        }
        return discordUserId;
    }

    private String createEventKey(
            Long alertId,
            String notificationType,
            Integer currentPrice
    ) {
        return "price-alert:" + alertId + ":" + notificationType.toLowerCase() + ":" + currentPrice;
    }
}
