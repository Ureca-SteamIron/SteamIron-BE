package norimaets.moduledomainrdb.repository;

import java.util.List;
import norimaets.moduledomainrdb.entity.Game;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GameRepository extends JpaRepository<Game, Long>, JpaSpecificationExecutor<Game> {
    // JpaSpecificationExecutor를 상속받으면 findAll(Specification, Sort) 메서드를 사용할 수 있습니다.

    // 현재 DB에 "할인 중"으로 저장된 게임들 = 직전 배치 시점의 할인 목록(diff의 기준).
    List<Game> findByDiscountPercentGreaterThan(int percent);

    /**
     * "유사한 게임" 섹션용 pg_trgm word_similarity 기반 검색 (GameService에서 1페이지에서만 호출).
     * "카운트"처럼 오타/유사 표기라 부분 문자열로는 못 잡는 경우를 여기서 보완한다.
     * similarity()는 검색어를 "게임 이름 전체"와 비교해서 이름이 길수록(단어 수가 많을수록) 점수가 낮게 나오는
     * 문제가 실측 확인됨 (예: similarity('카운트','메탈슈츠: 카운터 어택')=0.21로 임계값 미달).
     * word_similarity(검색어, 이름)는 이름 안에서 가장 잘 맞는 부분만 비교해서 훨씬 정확하다
     * (같은 케이스 word_similarity=0.67). 인자 순서 중요 — 검색어가 첫 번째.
     *
     * WHERE에 "NOT ILIKE" 제외가 반드시 필요하다 — 정확히 일치하는 이름은 word_similarity가 거의 1.0(최고점)이라
     * 이 조건이 없으면 LIMIT으로 가져오는 상위 N개가 전부 "이미 games에 있는 정확 일치 게임"으로 채워져버린다.
     * (실측: "Counter"는 부분 일치가 100건이라 이 조건 없이는 top 5가 전부 정확 일치로 채워져서
     * GameService의 중복 제거 로직을 통과할 후보가 하나도 안 남았음 — 즉 similarGames가 항상 텅 비었음)
     * "Country"/"Countess"류의 노이즈는 이 필터와 무관하게, 애초에 임계값(threshold) 미만이면 안 잡힌다.
     *
     * DB에 pg_trgm 확장(CREATE EXTENSION IF NOT EXISTS pg_trgm;)이 먼저 설치돼 있어야 한다 —
     * 이 확장 설치는 코드로 자동 보장한다 (DatabaseExtensionInitializer 참고).
     * 정렬 마지막에 game_id ASC를 붙여서 유사도·이름까지 같은 동점 케이스에서도 결과가 흔들리지 않게 한다.
     */
    @Query(
            value = "SELECT * FROM game g " +
                    "WHERE g.name NOT ILIKE CONCAT('%', :keyword, '%') " +
                    "   AND word_similarity(:keyword, g.name) > :threshold " +
                    "ORDER BY word_similarity(:keyword, g.name) DESC, g.name ASC, g.game_id ASC",
            countQuery = "SELECT count(*) FROM game g " +
                    "WHERE g.name NOT ILIKE CONCAT('%', :keyword, '%') " +
                    "   AND word_similarity(:keyword, g.name) > :threshold",
            nativeQuery = true
    )
    Page<Game> searchByNameSimilarity(@Param("keyword") String keyword, @Param("threshold") double threshold, Pageable pageable);
}