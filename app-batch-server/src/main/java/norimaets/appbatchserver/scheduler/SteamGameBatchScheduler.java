package norimaets.appbatchserver.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class SteamGameBatchScheduler {

    private final JobOperator jobOperator;
    private final Job steamTop100Job;

    @Scheduled(cron = "0 0 3 * * *")
    public void runSteamTop100Job() {
        LocalDate today = LocalDate.now();

        JobParameters jobParameters = new JobParametersBuilder()
                .addLocalDate("collectedDate", today)
                .addLocalDateTime("runAt", LocalDateTime.now())
                .toJobParameters();

        try {
            jobOperator.start(steamTop100Job, jobParameters);
            log.info("SteamTop100Job 실행 완료: {}", today);
        } catch (Exception e) {
            log.error("SteamTop100Job 실행 실패: {}", today, e);
        }
    }
}