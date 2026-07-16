package norimaets.appbatchserver.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/*
 *
 * 예시용 파일입니다!!
 *
 */
@Slf4j
@Component
public class SteamGameBatchScheduler {

    // 테스트를 위해 10분마다 실행되도록 임시 설정
    @Scheduled(fixedDelay = 600000)
    public void runPriceUpdateBatch() {
        log.info("⏰ 스팀 게임 가격 업데이트 스케줄러 실행 중...");
        // TODO: 향후 여기에 JobLauncher를 이용한 Batch Job 실행 로직 추가
    }
}
