package norimaets.moduledomainrdb.repository;

import norimaets.moduledomainrdb.entity.Game;
import norimaets.moduledomainrdb.entity.TopRanking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface TopRankingRepository extends JpaRepository<TopRanking, Long> {

    @Query("SELECT t.game FROM TopRanking t WHERE t.collectedDate = CURRENT_DATE ORDER BY t.rank ASC LIMIT 100")
    List<Game> findTodayTop100Games();

    List<TopRanking> findAllByCollectedDateOrderByRankAsc(LocalDate date);
}
