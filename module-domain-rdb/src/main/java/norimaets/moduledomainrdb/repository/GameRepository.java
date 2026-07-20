package norimaets.moduledomainrdb.repository;

import norimaets.moduledomainrdb.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface GameRepository extends JpaRepository<Game, Long>, JpaSpecificationExecutor<Game> {
    // JpaSpecificationExecutor를 상속받으면 findAll(Specification, Sort) 메서드를 사용할 수 있습니다.
}