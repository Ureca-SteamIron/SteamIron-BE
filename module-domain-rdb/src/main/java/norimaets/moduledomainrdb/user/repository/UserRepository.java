package norimaets.moduledomainrdb.user.repository;

import java.util.Optional;
import norimaets.moduledomainrdb.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByDiscordId(String discordId);
}
