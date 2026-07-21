package norimaets.appbatchserver.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import norimaets.appbatchserver.client.AppDetailsClient;
import norimaets.appbatchserver.client.SteamChartsClient;
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

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SteamCrawlingJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final SteamChartsClient steamChartsClient;
    private final AppDetailsClient appDetailsClient;
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
        List<SteamChartsClient.ChartRankDto> ranks = steamChartsClient.fetchTop100();

        if (ranks.isEmpty()) {
            log.warn("Steam Charts 응답이 비어있어 배치를 종료합니다.");
            return;
        }

        LocalDate today = LocalDate.now();
        List<Long> updatedGameIds = new ArrayList<>();
        int skipped = 0;

        for (SteamChartsClient.ChartRankDto dto : ranks) {
            if (dto.getAppid() == null || dto.getRank() == null) continue;

            Game game = resolveGame(dto.getAppid());

            if (game == null) {
                skipped++;
                log.warn("게임 정보 조회 실패로 랭킹에서 제외: appid={}, rank={}", dto.getAppid(), dto.getRank());
                continue;
            }

            upsertRanking(game, dto.getRank(), today);
            updatedGameIds.add(game.getId());
        }

        log.info("Top100 갱신 완료: {}건 (제외 {}건), 날짜={}", updatedGameIds.size(), skipped, today);
    }

    private void upsertRanking(Game game, Integer rank, LocalDate today) {
        topRankingRepository.findByGame_Id(game.getId())
                .ifPresentOrElse(
                        existing -> existing.updateRanking(rank, today),
                        () -> topRankingRepository.save(
                                TopRanking.builder()
                                        .game(game)
                                        .rank(rank)
                                        .collectedDate(today)
                                        .build()
                        )
                );
    }

    /**
     * game 테이블에 있으면 필수 정보(header_image, 가격) 누락 여부 확인 후 보완.
     * 없으면 appdetails로 신규 생성. 신규 생성도 실패하면 null 반환.
     */
    private Game resolveGame(Long appid) {
        return gameRepository.findById(appid)
                .map(this::fillMissingInfoIfNeeded)
                .orElseGet(() -> createNewGameOrNull(appid));
    }

    // 기존 게임이라도 header_image/가격 정보가 비어있으면 appdetails로 보완.
    // 정보가 이미 충분하면 절대 수정하지 않고 그대로 반환.
    private Game fillMissingInfoIfNeeded(Game existing) {
        boolean isMissingInfo = existing.getHeaderImage() == null
                || existing.getOriginalPrice() == null
                || existing.getFinalPrice() == null;

        if (!isMissingInfo) {
            return existing;
        }

        AppDetailsClient.AppDetail detail = appDetailsClient.fetchDetail(existing.getId());
        if (detail == null) {
            return existing; // 보완 실패해도 기존 데이터 그대로 유지
        }

        existing.updateIfPresent(
                detail.getName(),
                detail.getHeaderImage(),
                detail.getPriceOverview() != null ? detail.getPriceOverview().getInitial() : null,
                detail.getPriceOverview() != null ? detail.getPriceOverview().getFinalPrice() : null,
                detail.getPriceOverview() != null ? detail.getPriceOverview().getDiscountPercent() : null,
                detail.getIsFree()
        );

        return existing;
    }

    private Game createNewGameOrNull(Long appid) {
        AppDetailsClient.AppDetail detail = appDetailsClient.fetchDetail(appid);

        if (detail == null || detail.getName() == null) {
            return null;
        }

        return gameRepository.save(
                Game.builder()
                        .id(appid)
                        .name(detail.getName())
                        .headerImage(detail.getHeaderImage())
                        .originalPrice(detail.getPriceOverview() != null ? detail.getPriceOverview().getInitial() : null)
                        .finalPrice(detail.getPriceOverview() != null ? detail.getPriceOverview().getFinalPrice() : null)
                        .discountPercent(detail.getPriceOverview() != null ? detail.getPriceOverview().getDiscountPercent() : 0)
                        .isFree(Boolean.TRUE.equals(detail.getIsFree()))
                        .build()
        );
    }
}