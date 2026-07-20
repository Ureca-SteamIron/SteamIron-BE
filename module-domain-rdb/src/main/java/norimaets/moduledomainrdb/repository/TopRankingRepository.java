package norimaets.moduledomainrdb.repository;

import norimaets.moduledomainrdb.entity.Game;
import norimaets.moduledomainrdb.entity.TopRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TopRankingRepository extends JpaRepository<TopRanking, Long> {

    @Query("SELECT t.game FROM TopRanking t WHERE t.collectedDate = CURRENT_DATE ORDER BY t.rank ASC LIMIT 100")
    List<Game> findTodayTop100Games();

    List<TopRanking> findAllByCollectedDateOrderByRankAsc(LocalDate date);

    // 데이터가 있는 가장 최근 수집일. 오늘 수집 배치가 아직/실패로 안 돌았을 때 폴백용.
    @Query("SELECT MAX(t.collectedDate) FROM TopRanking t")
    Optional<LocalDate> findLatestCollectedDate();

    void deleteByCollectedDate(LocalDate collectedDate);
}
