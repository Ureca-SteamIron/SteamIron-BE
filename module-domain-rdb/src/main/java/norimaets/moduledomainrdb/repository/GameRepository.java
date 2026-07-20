package norimaets.moduledomainrdb.repository;

import java.util.List;
import norimaets.moduledomainrdb.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface GameRepository extends JpaRepository<Game, Long>, JpaSpecificationExecutor<Game> {
    // JpaSpecificationExecutor를 상속받으면 findAll(Specification, Sort) 메서드를 사용할 수 있습니다.

    // 현재 DB에 "할인 중"으로 저장된 게임들 = 직전 배치 시점의 할인 목록(diff의 기준).
    List<Game> findByDiscountPercentGreaterThan(int percent);
}