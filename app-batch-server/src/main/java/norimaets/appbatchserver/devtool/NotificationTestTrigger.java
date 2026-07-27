package norimaets.appbatchserver.devtool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import norimaets.appbatchserver.service.PriceAlertNotificationService;
import norimaets.moduledomainrdb.entity.Game;
import norimaets.moduledomainrdb.repository.GameRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 개발/시연용 수동 트리거. 실제 Steam 크롤링(DiscountCollectService.collect()) 없이
 * 특정 게임의 가격을 직접 지정해서 PriceAlertNotificationService부터 바로 검증한다.
 *
 * "notification-trigger" 프로필이 켜져 있을 때만 동작하므로, 평소 배포/실행에는 영향 없음.
 *
 * 사용법 (필수: --gameId, --finalPrice / 선택: --originalPrice, --discountPercent, --discountStarted):
 *   ./gradlew :app-batch-server:bootRun --args="--spring.profiles.active=notification-trigger --gameId=2561650 --finalPrice=15000 --originalPrice=20000 --discountPercent=25 --discountStarted=false"
 *
 * --discountStarted=true  → "할인 시작" 알림 조건으로 판정
 * --discountStarted=false(기본값) → "목표가 도달" 알림 조건으로 판정
 */
@Slf4j
@Component
@Profile("notification-trigger")
@RequiredArgsConstructor
public class NotificationTestTrigger implements ApplicationRunner {

    private final GameRepository gameRepository;
    private final PriceAlertNotificationService priceAlertNotificationService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Long gameId = Long.valueOf(requireOption(args, "gameId"));
        Integer finalPrice = Integer.valueOf(requireOption(args, "finalPrice"));
        Integer originalPrice = Integer.valueOf(optionOrDefault(args, "originalPrice", String.valueOf(finalPrice)));
        Integer discountPercent = Integer.valueOf(optionOrDefault(args, "discountPercent", "0"));
        boolean discountStarted = Boolean.parseBoolean(optionOrDefault(args, "discountStarted", "false"));

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("게임을 찾을 수 없습니다: gameId=" + gameId));
        game.updatePriceInfo(originalPrice, finalPrice, discountPercent, false);
        gameRepository.save(game);

        log.info("=== [notification-trigger] gameId={}, finalPrice={}, discountStarted={} ===",
                gameId, finalPrice, discountStarted);
        priceAlertNotificationService.process(game, discountStarted);
        log.info("=== [notification-trigger] 완료 ===");
    }

    private String requireOption(ApplicationArguments args, String name) {
        if (!args.containsOption(name)) {
            throw new IllegalArgumentException("--" + name + "=<값> 이 필요합니다");
        }
        return args.getOptionValues(name).get(0);
    }

    private String optionOrDefault(ApplicationArguments args, String name, String defaultValue) {
        return args.containsOption(name) ? args.getOptionValues(name).get(0) : defaultValue;
    }
}
