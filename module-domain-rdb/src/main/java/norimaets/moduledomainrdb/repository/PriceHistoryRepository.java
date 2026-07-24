package norimaets.moduledomainrdb.repository;

import java.util.List;
import norimaets.moduledomainrdb.entity.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {
    // 저장(save)만 있으면 배치엔 충분. 조회(그래프용)는 API 서버에서 별도 메서드로 추가.

    // 게임 상세 가격 변동 차트용: 해당 게임의 히스토리를 기록 시간 오름차순으로 반환.
    // 배치가 변동 있을 때만 기록해 포인트가 듬성듬성하므로 전체를 반환해도 부담이 적다.
    List<PriceHistory> findByGame_IdOrderByRecordedAtAsc(Long gameId);
}
