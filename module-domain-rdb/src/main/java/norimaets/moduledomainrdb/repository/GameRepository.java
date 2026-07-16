package norimaets.moduledomainrdb.repository;

import norimaets.moduledomainrdb.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameRepository extends JpaRepository<Game, Long> {
}
