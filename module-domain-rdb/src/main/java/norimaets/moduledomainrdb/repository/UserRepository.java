package norimaets.moduledomainrdb.repository;

import java.util.Optional;
import norimaets.moduledomainrdb.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByDiscordId(String discordId); // Discord 로그인

    Optional<User> findByLoginId(String loginId);      // 로컬 로그인(아이디)

    Optional<User> findByEmail(String email);          // 이메일 중복 체크 등

    boolean existsByLoginId(String loginId);           // 아이디 중복 체크
}
