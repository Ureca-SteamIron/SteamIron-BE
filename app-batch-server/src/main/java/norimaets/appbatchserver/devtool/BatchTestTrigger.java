package norimaets.appbatchserver.devtool;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import norimaets.appbatchserver.service.DiscountCollectService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 개발/시연용 수동 트리거. 6시간 주기 스케줄(DiscountCollectScheduler)을 기다리지 않고
 * 실제 배치(Steam 전체 할인 목록 수집 → diff 판정 → 알림 발행)를 그 자리에서 한 번 실행한다.
 *
 * "batch-trigger" 프로필이 켜져 있을 때만 동작하므로, 평소 배포/실행에는 영향 없음.
 * 실제 Steam API를 호출하므로 수 분 정도 걸릴 수 있다(할인 항목 수에 비례, 페이지당 1.5초 대기).
 *
 * 사용법:
 *   ./gradlew :app-batch-server:bootRun --args="--spring.profiles.active=batch-trigger"
 */
@Slf4j
@Component
@Profile("batch-trigger")
@RequiredArgsConstructor
public class BatchTestTrigger implements CommandLineRunner {

    private final DiscountCollectService discountCollectService;

    @Override
    public void run(String... args) {
        log.info("=== [batch-trigger] 배치(collect) 수동 실행 시작 ===");
        discountCollectService.collect();
        log.info("=== [batch-trigger] 배치(collect) 수동 실행 완료 ===");
    }
}
