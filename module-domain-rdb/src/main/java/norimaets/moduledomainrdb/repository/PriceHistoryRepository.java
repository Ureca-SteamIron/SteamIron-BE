package norimaets.moduledomainrdb.repository;

import norimaets.moduledomainrdb.entity.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {
    // 저장(save)만 있으면 배치엔 충분. 조회(그래프용)는 API 서버에서 별도 메서드로 추가.
}
