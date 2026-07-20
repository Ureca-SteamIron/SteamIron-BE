package norimaets.moduledomainrdb.repository;

import java.util.Optional;
import norimaets.moduledomainrdb.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByUserId(Long userId);

    void deleteByToken(String token);
}
