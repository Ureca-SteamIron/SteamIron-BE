package norimaets.moduledomainrdb.repository;

import java.util.Optional;
import norimaets.moduledomainrdb.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByUserId(Long userId);

    void deleteByToken(String token);

    // 회원 탈퇴 시 해당 유저의 리프레시 토큰 삭제
    void deleteByUserId(Long userId);
}
