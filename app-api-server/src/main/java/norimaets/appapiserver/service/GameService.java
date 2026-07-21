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
        // 1. Top 100 기준 날짜 결정: 오늘 수집분이 없으면(배치 미실행/실패) 가장 최근 수집일로 폴백.
        //    이렇게 안 하면 오늘 데이터가 없는 날 메인 화면이 통째로 빈다.
        LocalDate targetDate = topRankingRepository.findLatestCollectedDate().orElse(null);
        if (targetDate == null) return Collections.emptyList(); // 수집 이력이 아예 없는 경우

        List<TopRanking> todayRankings = topRankingRepository.findAllByCollectedDateOrderByRankAsc(targetDate);
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

    private static final int SEARCH_MIN_KEYWORD_LENGTH = 2;

    /**
     * 키워드로 전체 게임을 검색한다 (top100 범위 제한 없음).
     * getFilteredTop100Games()와 달리 rankedGameIds로 좁히지 않고 gameRepository 전체를 대상으로 한다.
     * 짧은 키워드(1자)는 결과가 너무 많아 느려지므로 최소 길이를 강제한다 (FE도 동일 기준으로 막지만, 다른 경로로
     * API를 직접 호출할 수도 있으니 BE에서도 방어한다).
     */
    @Transactional(readOnly = true)
    public List<GameSimpleResponse> searchGames(String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.length() < SEARCH_MIN_KEYWORD_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_SEARCH_KEYWORD);
        }

        Specification<Game> spec = (root, query, builder) ->
                builder.like(builder.lower(root.get("name")), "%" + normalizedKeyword.toLowerCase() + "%");

        List<Game> games = gameRepository.findAll(spec);

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
