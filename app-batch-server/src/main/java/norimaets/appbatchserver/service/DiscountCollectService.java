package norimaets.appbatchserver.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import norimaets.appbatchserver.collector.SteamDiscountCollector;
import norimaets.appbatchserver.dto.DiscountSnapshot;
import norimaets.appbatchserver.dto.SteamDiscountItem;
import norimaets.moduledomainrdb.entity.Game;
import norimaets.moduledomainrdb.entity.PriceHistory;
import norimaets.moduledomainrdb.repository.GameRepository;
import norimaets.moduledomainrdb.repository.PriceHistoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 할인 수집 배치의 핵심.
 * 이번에 받아온 "전체 할인 목록"과, DB에 저장된 "직전 할인 목록"을 비교(diff)해서
 *   ① 새로 등장  → 할인 시작 : 현재가 갱신 + 히스토리 저장
 *   ② 여전히 있음(가격 변동) → 할인 변경 : 현재가 갱신 + 히스토리 저장
 *   ③ 사라짐     → 할인 종료 : 정가로 복원 + 히스토리 저장
 * 가격이 그대로면 아무것도 하지 않는다(중복 히스토리 방지).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiscountCollectService {

    private final SteamDiscountCollector collector;
    private final GameRepository gameRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final PriceAlertNotificationService priceAlertNotificationService;

    @Transactional
    public void collect() {
        // 1. 이번 할인 목록 수집 → appId 기준 Map
        //    collector가 sort_by=Name_ASC로 목록을 이름순 고정 정렬해 훑으므로 누락/중복이 거의 없다(실측 수집률 99.7%).
        //    그래도 남는 소수 누락이 "할인 종료" 오판으로 번지지 않게, complete 판정(고유수≥total_count·97%)으로 한 번 더 가드한다.
        DiscountSnapshot snapshot = collector.fetchAllDiscounts();
        List<SteamDiscountItem> current = snapshot.items();
        Map<Long, SteamDiscountItem> currentMap = current.stream()
                .collect(Collectors.toMap(SteamDiscountItem::appId, item -> item, (a, b) -> a));

        // 2. 우리 DB에 존재하는 게임만 추적(모르는 게임은 top100/appdetails 배치가 채운다)
        //    한 번의 쿼리로 이번 목록에 해당하는 Game들을 미리 로딩 (findById 반복 방지)
        Map<Long, Game> dbGames = gameRepository.findAllById(currentMap.keySet()).stream()
                .collect(Collectors.toMap(Game::getId, game -> game));

        // 3. 직전 할인 목록 = DB에서 현재 할인중으로 찍힌 게임들 (diff의 기준)
        List<Game> prevDiscounted = gameRepository.findByDiscountPercentGreaterThan(0);

        int started = 0, changed = 0, ended = 0;
        // 이번 배치에서 실제로 가격이나 할인율이 변경된 게임만 보관, 모든 게임 알림 조회하면 DB 부하가 커짐
        List<Game> priceChangedGames = new ArrayList<>();
        Set<Long> discountStartedGameIds = new HashSet<>();

        // 4. 이번 목록 순회 → 시작/변경 판정
        for (SteamDiscountItem item : current) {
            Game game = dbGames.get(item.appId());
            if (game == null) continue; // 우리 DB에 없는 게임 → 추적 대상 아님

            boolean priceChanged = !Objects.equals(game.getFinalPrice(), item.finalPrice())
                    || !Objects.equals(game.getDiscountPercent(), item.discountPercent());
            if (!priceChanged) continue; // 가격 그대로면 히스토리 남기지 않음

            boolean wasDiscounted = game.getDiscountPercent() != null && game.getDiscountPercent() > 0;

            // 정가: 파싱된 값이 있으면 쓰고, 없으면 기존 DB 정가 유지
            Integer original = item.originalPrice() != null ? item.originalPrice() : game.getOriginalPrice();
            game.updatePriceInfo(original, item.finalPrice(), item.discountPercent(), false);
            saveHistory(game, item.finalPrice(), item.discountPercent());

            priceChangedGames.add(game);

            if (wasDiscounted) {
                changed++;
            } else {
                discountStartedGameIds.add(game.getId());
                started++;
            }
        }

        // 5. 직전엔 할인이었는데 이번 목록엔 없음 → 할인 종료 → 정가 복원 + 히스토리
        //    ⚠ 완전 수집일 때만! 부분 수집이면 안 받아온 게임을 전부 종료로 오판해 데이터가 오염된다.
        if (snapshot.complete()) {
            for (Game game : prevDiscounted) {
                if (currentMap.containsKey(game.getId())) continue; // 여전히 할인중이면 skip

                Integer original = game.getOriginalPrice() != null ? game.getOriginalPrice() : game.getFinalPrice();
                game.updatePriceInfo(original, original, 0, false); // 정가 = 현재가, 할인율 0
                saveHistory(game, original, 0);
                priceChangedGames.add(game);
                ended++;
            }
        } else {
            log.warn("수집이 불완전({}/{})하여 '할인 종료' 처리를 건너뜁니다. (데이터 오염 방지)",
                    current.size(), snapshot.totalCount());
        }

        log.info("할인 수집 완료 → 시작 {}, 변경 {}, 종료 {} (수신 {}건)", started, changed, ended, current.size());
        // 배치가 판정 서비스 호출
        for (Game game : priceChangedGames) {
            priceAlertNotificationService.process(
                    game,
                    discountStartedGameIds.contains(game.getId())
            );
        }
    }

    private void saveHistory(Game game, Integer price, Integer discountPercent) {
        priceHistoryRepository.save(PriceHistory.builder()
                .game(game)
                .price(price)
                .discountPercent(discountPercent)
                .build());
    }
}
