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
    public void process(Game game) {
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
            processAlert(alert, game, currentPrice);
        }
    }

    private void processAlert(
            PriceAlert alert,
            Game game,
            Integer currentPrice
    ) {
        User user = alert.getUser();

        if (!user.isDiscordNotificationEnabled()) {
            return;
        }

        Integer targetPrice = alert.getTargetPrice();

        if (targetPrice == null || currentPrice > targetPrice) {
            return;
        }

        // 중복 알람 확인
        if (Objects.equals(alert.getLastNotifiedPrice(), currentPrice)) {
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

        String eventKey = createEventKey(alert.getId(), currentPrice);

        DiscordDmRequest request = new DiscordDmRequest(
                eventKey,
                alert.getId(),
                user.getId(),
                discordUserId,
                game.getId(),
                game.getName(),
                targetPrice,
                currentPrice,
                game.getDiscountPercent()
        );

        DiscordDmResponse response =
                discordNotificationClient.sendDm(request);

        if (response != null && response.isCompleted()) {
            alert.updateLastNotified(
                    currentPrice,
                    LocalDateTime.now()
            );

            log.info(
                    "가격 알림 발송 완료: eventKey={}, status={}",
                    eventKey,
                    response.status()
            );

            return;
        }

        log.warn(
                "가격 알림 발송 미완료: eventKey={}, status={}",
                eventKey,
                response != null ? response.status() : null
        );
    }

    private String createEventKey(
            Long alertId,
            Integer currentPrice
    ) {
        return "price-alert:" + alertId + ":" + currentPrice;
    }
}