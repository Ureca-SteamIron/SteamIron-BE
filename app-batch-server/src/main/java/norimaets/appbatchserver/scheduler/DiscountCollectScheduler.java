package norimaets.appbatchserver.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import norimaets.appbatchserver.service.DiscountCollectService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 할인 수집 배치 스케줄러.
 * 6시간마다(0/6/12/18시 정각) 전체 할인 목록을 수집해 diff → 가격/히스토리 갱신.
 * 스팀 세일은 보통 새벽 2~3시에 교체되므로, 00시 실행분이 그날 교체를 커버한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DiscountCollectScheduler {

    private final DiscountCollectService discountCollectService;

    // 초 분 시 일 월 요일 — 매일 03, 09, 15, 21시 정각.
    // 스팀 할인이 새벽 2~3시에 교체되므로 03시 실행분이 그날 교체를 곧바로 반영한다.
    @Scheduled(cron = "0 0 3,9,15,21 * * *", zone = "Asia/Seoul")
    public void run() {
        long begin = System.currentTimeMillis();
        log.info("⏰ 할인 수집 배치 시작");
        try {
            discountCollectService.collect();
        } catch (Exception e) {
            // 한 번 실패해도 다음 6시간 뒤 재시도됨. 배치 프로세스는 죽지 않게 삼킨다.
            log.error("할인 수집 배치 실패", e);
        }
        log.info("✅ 할인 수집 배치 종료 ({}ms)", System.currentTimeMillis() - begin);
    }
}
