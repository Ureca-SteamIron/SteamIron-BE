package norimaets.appapiserver.service;

import jakarta.persistence.criteria.Join;
import lombok.RequiredArgsConstructor;
import norimaets.appapiserver.common.exception.CustomException;
import norimaets.appapiserver.common.exception.ErrorCode;
import norimaets.appapiserver.dto.request.GameFilterRequest;
import norimaets.appapiserver.dto.response.GameDetailResponse;
import norimaets.appapiserver.dto.response.GameSimpleResponse;
import norimaets.moduledomainrdb.entity.Game;
import norimaets.moduledomainrdb.entity.GameGenre;
import norimaets.moduledomainrdb.entity.Genre;
import norimaets.moduledomainrdb.entity.TopRanking;
import norimaets.moduledomainrdb.repository.GameRepository;
import norimaets.moduledomainrdb.repository.TopRankingRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;
    private final TopRankingRepository topRankingRepository;

    @Transactional(readOnly = true)
    public List<GameSimpleResponse> getTop100Games() {

        List<Game> top100Games = topRankingRepository.findTodayTop100Games();

        return top100Games.stream()
                .map(GameSimpleResponse::from)
                .collect(Collectors.toList());
    }

    public List<GameSimpleResponse> getFilteredTop100Games(GameFilterRequest request) {
        // 1. 오늘자 Top 100 데이터를 '랭킹(rank) 오름차순'으로 먼저 가져옵니다.
        List<TopRanking> todayRankings = topRankingRepository.findAllByCollectedDateOrderByRankAsc(LocalDate.now());
        if (todayRankings.isEmpty()) return Collections.emptyList();

        // 2. 랭킹 순서가 보장된 게임 ID 리스트를 추출합니다.
        List<Long> rankedGameIds = todayRankings.stream()
                .map(tr -> tr.getGame().getId())
                .collect(Collectors.toList());

        // 3. 공통 필터 조립 + IN 절로 해당 ID의 게임만 조회하도록 추가합니다.
        Specification<Game> spec = buildBaseFilterSpecification(request)
                .and((root, query, builder) -> root.get("id").in(rankedGameIds));

        // 4. 요청된 정렬 기준 확인 (기본 'popular'일 경우 DB 정렬 생략)
        boolean isDefaultSort = request.getSort() == null || request.getSort().equals("popular");
        Sort dbSort = isDefaultSort ? Sort.unsorted() : getSort(request.getSort());

        // 5. DB에서 필터링된 게임들을 가져옵니다.
        List<Game> games = gameRepository.findAll(spec, dbSort);

        // 6. 만약 기본 정렬(인기순/랭킹순)이라면, 2번에서 만든 rankedGameIds 순서에 맞춰서 메모리에서 재배치합니다!
        if (isDefaultSort) {
            games.sort(Comparator.comparingInt(game -> rankedGameIds.indexOf(game.getId())));
        }

        return games.stream().map(GameSimpleResponse::from).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GameDetailResponse getGameDetail(Long gameId, Long userId) {

        // 1. 게임 기본 정보 조회
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new CustomException(ErrorCode.GAME_NOT_FOUND));

        // 2. AI 분석 정보 조회 (아직 분석 안 된 게임일 수도 있으니 Optional 처리)
//        GameAiAnalysis aiAnalysis = gameAiAnalysisRepository.findById(gameId).orElse(null);

        // 3. 현재 유저의 찜 여부 확인 (userId가 null이면 비로그인이므로 false)
        boolean isWishlisted = false;
//        if (userId != null) {
//            isWishlisted = wishListRepository.existsByUserIdAndGameId(userId, gameId);
//        }

        // 4. 모든 데이터를 DTO 바구니에 담아서 반환
        return GameDetailResponse.of(game,
//                aiAnalysis,
                isWishlisted);
    }

    // ---------------------private---------------------
    private Specification<Game> buildBaseFilterSpecification(GameFilterRequest req) {
        Specification<Game> spec = Specification.unrestricted();

        if (req.getGenre() != null && !"all".equalsIgnoreCase(req.getGenre())) {
            spec = spec.and((root, query, builder) -> {
                Join<Game, GameGenre> gameGenreJoin = root.join("gameGenres");
                Join<GameGenre, Genre> genreJoin = gameGenreJoin.join("genre");
                return builder.equal(genreJoin.get("name"), req.getGenre());
            });
        }

        if ("free".equalsIgnoreCase(req.getPriceType())) {
            spec = spec.and((root, query, builder) -> builder.isTrue(root.get("isFree")));
        } else if ("paid".equalsIgnoreCase(req.getPriceType())) {
            spec = spec.and((root, query, builder) -> builder.isFalse(root.get("isFree")));
        }

        if (req.getMinPrice() != null) {
            spec = spec.and((root, query, builder) -> builder.greaterThanOrEqualTo(root.get("finalPrice"), req.getMinPrice()));
        }
        if (req.getMaxPrice() != null) {
            spec = spec.and((root, query, builder) -> builder.lessThanOrEqualTo(root.get("finalPrice"), req.getMaxPrice()));
        }
        if (req.getMinDiscount() != null && req.getMinDiscount() > 0) {
            spec = spec.and((root, query, builder) -> builder.greaterThanOrEqualTo(root.get("discountPercent"), req.getMinDiscount()));
        }
        if (req.isSale()) {
            spec = spec.and((root, query, builder) -> builder.greaterThan(root.get("discountPercent"), 0));
        }

        return spec;
    }

    private Sort getSort(String sortType) {
        switch (sortType != null ? sortType : "popular") {
            case "price_asc":
                return Sort.by(Sort.Direction.ASC, "finalPrice");
            case "price_desc":
                return Sort.by(Sort.Direction.DESC, "finalPrice");
            case "discount_desc":
                return Sort.by(Sort.Direction.DESC, "discountPercent");
            case "name_asc":
                return Sort.by(Sort.Direction.ASC, "name");
            case "name_desc":
                return Sort.by(Sort.Direction.DESC, "name");
            default:
                return Sort.by(Sort.Direction.ASC, "id");
        }
    }
}
