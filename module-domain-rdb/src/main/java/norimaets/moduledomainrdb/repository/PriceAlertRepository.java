package norimaets.moduledomainrdb.repository;

import java.util.List;
import java.util.Optional;
import norimaets.moduledomainrdb.entity.PriceAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {

    // ── 배치 판정용 ──────────────────────────────
    // 특정 게임에 걸린 "활성" 알림들. 가격이 바뀐 게임마다 이걸로 대상자를 찾는다.
    @Query("""
        SELECT alert
        FROM PriceAlert alert
        JOIN FETCH alert.user
        WHERE alert.game.id = :gameId
          AND alert.isActive = true
        """)
    List<PriceAlert> findByGame_IdAndIsActiveTrue(
            @Param("gameId") Long gameId
    );

    // ── 저장 API용 ──────────────────────────────
    // 내 알림 목록 (화면 표시용 → 게임 정보까지 fetch join으로 함께 로딩, N+1 방지)
    @Query("SELECT a FROM PriceAlert a JOIN FETCH a.game WHERE a.user.id = :userId")
    List<PriceAlert> findAllByUserIdWithGame(@Param("userId") Long userId);

    // 같은 유저-게임 알림 단건 (중복 방지 / 갱신용)
    Optional<PriceAlert> findByUser_IdAndGame_Id(Long userId, Long gameId);

    // 같은 게임에 이미 알림 설정했는지 (생성 시 중복 체크)
    boolean existsByUser_IdAndGame_Id(Long userId, Long gameId);
}
