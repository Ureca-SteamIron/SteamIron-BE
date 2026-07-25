package norimaets.appapiserver.service;

import lombok.RequiredArgsConstructor;
import norimaets.appapiserver.client.AppDetailsClient;
import norimaets.appapiserver.common.exception.CustomException;
import norimaets.appapiserver.common.exception.ErrorCode;
import norimaets.appapiserver.common.sort.GameNameSort;
import norimaets.appapiserver.dto.request.GameFilterRequest;
import norimaets.appapiserver.dto.response.AiSummaryResponse;
import norimaets.appapiserver.dto.response.GameDetailResponse;
import norimaets.appapiserver.dto.response.GameSearchResponse;
import norimaets.appapiserver.dto.response.GameSimpleResponse;
import norimaets.appapiserver.dto.response.PriceHistoryResponse;
import norimaets.appapiserver.specification.GameSpecs;
import norimaets.moduledomainrdb.entity.*;
import norimaets.moduledomainrdb.repository.GameRepository;
import norimaets.moduledomainrdb.repository.PriceHistoryRepository;
import norimaets.moduledomainrdb.repository.TopRankingRepository;
import norimaets.moduledomainrdb.repository.WishListRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;
    private final TopRankingRepository topRankingRepository;
    private final AppDetailsClient appDetailsClient;

    private final WishListRepository wishListRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final GeminiService geminiService;

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
        //    baseFilter()에 데모/가격 미수집 게임 제외 조건까지 포함되어 있어 top100에도 동일하게 적용된다.
        Specification<Game> spec = GameSpecs.baseFilter(request)
                .and((root, query, builder) -> root.get("id").in(rankedGameIds));

        // 4. 요청된 정렬 기준 확인 (기본 'popular'일 경우 DB 정렬 생략)
        boolean isDefaultSort = request.getSort() == null || request.getSort().equals("popular");
        // 이름순은 "한글→영어→기타" 그룹 우선순위가 필요해 DB 콜레이션만으론 부족하다 → 메모리에서 Comparator로 정렬한다.
        // (Top100 한정이라 최대 100개, 페이지네이션도 없어 메모리 정렬이 안전하다.)
        Comparator<Game> nameComparator = nameGroupComparator(request.getSort());
        boolean isNameSort = nameComparator != null;
        Sort dbSort = (isDefaultSort || isNameSort) ? Sort.unsorted() : GameSpecs.resolveSort(request.getSort());

        // 5. DB에서 필터링된 게임들을 가져옵니다.
        List<Game> games = gameRepository.findAll(spec, dbSort);

        // 6. 만약 기본 정렬(인기순/랭킹순)이라면, 2번에서 만든 rankedGameIds 순서에 맞춰서 메모리에서 재배치합니다!
        if (isDefaultSort) {
            games.sort(Comparator.comparingInt(game -> rankedGameIds.indexOf(game.getId())));
        } else if (isNameSort) {
            games.sort(nameComparator);
        }

        return games.stream().map(GameSimpleResponse::from).collect(Collectors.toList());
    }

    private static final int SEARCH_MIN_KEYWORD_LENGTH = 2;
    private static final int SEARCH_DEFAULT_PAGE_SIZE = 25; // 스팀 검색 결과와 동일하게 페이지당 25개
    private static final int SEARCH_MAX_PAGE_SIZE = 100; // size를 과도하게 크게 넘기는 것 방지
    private static final double SEARCH_SIMILARITY_THRESHOLD = 0.3; // pg_trgm word_similarity 임계값 (0~1, 낮을수록 더 관대하게 매칭)
    private static final int SEARCH_SIMILAR_GAMES_LIMIT = 5; // "유사한 게임" 섹션에 보여줄 최대 개수

    /**
     * 키워드로 전체 게임을 검색한다 (top100 범위 제한 없음).
     * getFilteredTop100Games()와 달리 rankedGameIds로 좁히지 않고 gameRepository 전체를 대상으로 한다.
     * 짧은 키워드(1자)는 결과가 너무 많아 느려지므로 최소 길이를 강제한다 (FE도 동일 기준으로 막지만, 다른 경로로
     * API를 직접 호출할 수도 있으니 BE에서도 방어한다).
     *
     * 검색 정책: games(부분 일치)와 similarGames(pg_trgm 유사 검색)를 항상 분리해서 반환한다.
     * - games: 부분 일치 결과. 페이지네이션 대상.
     * - similarGames: 유사도 상위 N개. games가 1페이지 안에 다 들어갈 때만(=검색이 이미 충분히 잘 안 됐을 때만)
     *   채운다. games가 2페이지 이상이면(=부분 일치가 이미 많으면) "유사한 게임"은 불필요하므로 비워둔다.
     *   GameRepository.searchByNameSimilarity()가 SQL 단에서 이미 부분 일치 게임을 제외하므로
     *   games와 절대 안 겹친다 — 별도 중복 제거가 필요 없다.
     * 두 결과를 하나로 합쳐서 정렬하지 않고 화면에서 섹션을 분리하는 이유는, "Counter" 검색에
     * "Country"류가 섞이는 걸 막으면서도 "카운트"↔"카운터"처럼 부분 일치가 있어도 유사 게임을
     * 놓치지 않기 위해서다 (기존엔 부분 일치가 있으면 유사 검색 자체를 안 돌려서 이런 케이스를 놓쳤음).
     *
     * filterRequest(장르/가격/할인/정렬)는 games에만 적용한다. similarGames는 추천 성격의
     * 소규모(최대 5개) 리스트라 필터까지 걸면 대부분 사라져서 의미가 없어지므로 필터 없이 그대로 둔다.
     */
    @Transactional(readOnly = true)
    public GameSearchResponse searchGames(String keyword, Integer page, Integer size, GameFilterRequest filterRequest) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.length() < SEARCH_MIN_KEYWORD_LENGTH) {
            throw new CustomException(ErrorCode.INVALID_SEARCH_KEYWORD);
        }

        int safePage = (page == null || page < 0) ? 0 : page;
        int safeSize = (size == null) ? SEARCH_DEFAULT_PAGE_SIZE : Math.min(Math.max(size, 1), SEARCH_MAX_PAGE_SIZE);

        // games: 부분 일치 + 공통 필터(장르/가격/할인, 데모 제외 포함) 결합. top100과 동일한 정렬 옵션을 지원하되
        // id 타이브레이커를 더해서(동점 케이스 대비) 페이지네이션 결과가 완전히 결정적으로 유지되게 한다.
        Specification<Game> spec = GameSpecs.baseFilter(filterRequest)
                .and((root, query, builder) ->
                        builder.like(builder.lower(root.get("name")), "%" + normalizedKeyword.toLowerCase() + "%"));

        // 이름순은 "한글→영어→기타" 그룹 우선순위가 필요해 Sort 객체로 표현할 수 없다.
        // 페이지네이션 때문에 DB 정렬은 필수라, 이 경우만 Specification의 query.orderBy에 CASE 식을 직접 심고
        // Pageable에는 정렬을 싣지 않는다. 나머지 정렬(가격/할인 등)은 기존 Sort 경로를 그대로 쓴다.
        String sortType = filterRequest.getSort();

        Pageable containsPageable;
        if (GameNameSort.isNameSort(sortType)) {
            // 이름순은 spec의 query.orderBy에 그룹 CASE를 심어 처리한다 (Pageable엔 정렬 미탑재).
            spec = spec.and(GameNameSort.orderSpec("name_desc".equals(sortType)));
            containsPageable = PageRequest.of(safePage, safeSize);
        } else {
            Sort sort = GameSpecs.resolveSort(sortType).and(Sort.by(Sort.Direction.ASC, "id"));
            containsPageable = PageRequest.of(safePage, safeSize, sort);
        }
        Page<Game> exactMatches = gameRepository.findAll(spec, containsPageable);

        List<GameSimpleResponse> similarGames = List.of();
        // games가 이미 여러 페이지(2페이지 이상)면 검색이 충분히 잘 된 것이므로 "유사한 게임"은 불필요.
        // 1페이지(=결과가 한 페이지에 다 들어갈 만큼 적을 때)에서만 보여준다.
        if (safePage == 0 && exactMatches.getTotalPages() <= 1) {
            // similarGames: 정렬이 word_similarity() 계산식 기준이라 Pageable에는 정렬을 안 싣는다.
            Pageable similarityPageable = PageRequest.of(0, SEARCH_SIMILAR_GAMES_LIMIT);
            similarGames = gameRepository.searchByNameSimilarity(normalizedKeyword, SEARCH_SIMILARITY_THRESHOLD, similarityPageable)
                    .getContent().stream()
                    .map(GameSimpleResponse::from)
                    .collect(Collectors.toList());
        }

        return GameSearchResponse.of(exactMatches.map(GameSimpleResponse::from), similarGames);
    }

    // AI 요약(Gemini 호출)은 여기서 하지 않는다 — 수 초씩 걸려서 상세 정보 응답 전체를
    // 붙들고 있었기 때문에 getAiSummary()로 분리했다. 프론트가 이 응답과 별도로(병렬로) 호출한다.
    @Transactional(readOnly = true)
    public GameDetailResponse getGameDetail(Long gameId, Long userId) {

        // 1. 게임 기본 정보 조회
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new CustomException(ErrorCode.GAME_NOT_FOUND));

        // 2. 현재 유저의 찜 여부 확인 (userId가 null이면 비로그인이므로 false)
        boolean isWishlisted = false;
        if (userId != null) {
            isWishlisted = wishListRepository.existsByUser_IdAndGame_Id(userId, gameId);
        }

        // 3. 이 게임을 찜한 전체 유저 수 (좋아요 개수처럼 노출)
        long wishlistCount = wishListRepository.countByGame_Id(gameId);

        // 4. 모든 데이터를 DTO 바구니에 담아서 반환
        return GameDetailResponse.of(game, isWishlisted, wishlistCount);
    }

    // AI 요약 전용 조회. 캐싱은 하지 않으므로(팀 결정) 호출할 때마다 Gemini를 새로 호출한다.
    @Transactional(readOnly = true)
    public AiSummaryResponse getAiSummary(Long gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new CustomException(ErrorCode.GAME_NOT_FOUND));

        String aiSummary = geminiService.generateGameSummary(
                game.getName(),
                game.getOriginalPrice(),
                game.getDiscountPercent()
        );

        return AiSummaryResponse.of(aiSummary);
    }

    // 게임 상세 가격 변동 차트용: 해당 게임의 가격 히스토리를 시간 오름차순으로 반환.
    // 배치가 변동 있을 때만 기록하므로 포인트가 없으면 빈 리스트다(FE는 '기록 없음'으로 처리).
    @Transactional(readOnly = true)
    public List<PriceHistoryResponse> getPriceHistory(Long gameId) {
        if (!gameRepository.existsById(gameId)) {
            throw new CustomException(ErrorCode.GAME_NOT_FOUND);
        }

        return priceHistoryRepository.findByGame_IdOrderByRecordedAtAsc(gameId).stream()
                .map(PriceHistoryResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 이름순 정렬이면 그룹 우선순위(한글→영어→기타) Comparator를, 아니면 null을 반환한다.
     * null이면 이름순이 아니라는 뜻이라 호출부에서 기존 DB 정렬 경로를 탄다.
     */
    private Comparator<Game> nameGroupComparator(String sortType) {
        if ("name_asc".equals(sortType)) return GameNameSort.ascComparator();
        if ("name_desc".equals(sortType)) return GameNameSort.descComparator();
        return null;
    }

    @Transactional
    public GameDetailResponse refreshGame(Long appId, Long userId) {
        Game game = gameRepository.findById(appId)
                .orElseThrow(() -> new CustomException(ErrorCode.GAME_NOT_FOUND));

        AppDetailsClient.AppDetail detail = appDetailsClient.fetchDetail(appId);
        if (detail == null) {
            throw new CustomException(ErrorCode.STEAM_API_FETCH_FAILED);
        }

        Integer originalPrice = null;
        Integer finalPrice = null;
        Integer discountPercent = 0;

        if (detail.getPriceOverview() != null) {
            originalPrice = detail.getPriceOverview().getInitial() / 100;
            finalPrice = detail.getPriceOverview().getFinalPrice() / 100;
            discountPercent = detail.getPriceOverview().getDiscountPercent();

            if (discountPercent != null) {
                if (discountPercent >= 100) {
                    finalPrice = 0;
                } else if (discountPercent == 0) {
                    finalPrice = originalPrice; // 할인 없음이면 정가와 동일해야 함
                }
            }
        }

        game.updateFromSteam(
                detail.getName(),
                detail.getHeaderImage(),
                Boolean.TRUE.equals(detail.getIsFree()),
                originalPrice,
                finalPrice,
                discountPercent
        );

        return getGameDetail(appId, userId);
    }

}