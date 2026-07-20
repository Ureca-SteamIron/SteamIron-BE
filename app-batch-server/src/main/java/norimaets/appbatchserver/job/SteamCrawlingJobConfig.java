package norimaets.appbatchserver.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import norimaets.appbatchserver.client.SteamSpyClient;
import norimaets.appbatchserver.dto.SteamSpyGameDto;
import norimaets.moduledomainrdb.entity.Game;
import norimaets.moduledomainrdb.entity.TopRanking;
import norimaets.moduledomainrdb.repository.GameRepository;
import norimaets.moduledomainrdb.repository.TopRankingRepository;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SteamCrawlingJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final SteamSpyClient steamSpyClient;
    private final GameRepository gameRepository;
    private final TopRankingRepository topRankingRepository;

    @Bean
    public Job steamTop100Job() {
        return new JobBuilder("steamTop100Job", jobRepository)
                .start(steamTop100Step())
                .build();
    }

    @Bean
    public Step steamTop100Step() {
        return new StepBuilder("steamTop100Step", jobRepository)
                .tasklet(steamTop100Tasklet(), transactionManager)
                .build();
    }

    @Bean
    public Tasklet steamTop100Tasklet() {
        return (contribution, chunkContext) -> {
            executeCrawling();
            return RepeatStatus.FINISHED;
        };
    }

    @Transactional
    public void executeCrawling() {
        Map<String, SteamSpyGameDto> response = steamSpyClient.fetchTop100();

        if (response == null || response.isEmpty()) {
            log.warn("SteamSpy 응답이 비어있어 배치를 종료합니다.");
            return;
        }

        LocalDate today = LocalDate.now();
        List<TopRanking> rankings = new ArrayList<>();
        int rank = 1;

        for (SteamSpyGameDto dto : response.values()) {
            if (dto.getAppid() == null) continue;

            Game game = upsertGame(dto);

            rankings.add(
                    TopRanking.builder()
                            .game(game)
                            .rank(rank++)
                            .collectedDate(today)
                            .build()
            );

            if (rank > 100) break;
        }

        topRankingRepository.deleteByCollectedDate(today);
        topRankingRepository.saveAll(rankings);

        log.info("Steam Top100 갱신 완료: {}건, 날짜={}", rankings.size(), today);
    }

    private Game upsertGame(SteamSpyGameDto dto) {
        boolean isFree = dto.getPrice() != null && dto.getPrice() == 0;

        return gameRepository.findById(dto.getAppid())
                .map(existing -> {
                    existing.updatePriceInfo(
                            dto.getInitialprice(),
                            dto.getPrice(),
                            dto.getDiscount(),
                            isFree
                    );
                    return existing;
                })
                .orElseGet(() -> gameRepository.save(
                        Game.builder()
                                .id(dto.getAppid())
                                .name(dto.getName())
                                .originalPrice(dto.getInitialprice())
                                .finalPrice(dto.getPrice())
                                .discountPercent(dto.getDiscount())
                                .isFree(isFree)
                                .build()
                ));
    }
}