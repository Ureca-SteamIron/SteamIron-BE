package norimaets.moduledomainrdb.repository;

import norimaets.moduledomainrdb.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 디스코드 ID로 유저를 찾는 기본 쿼리 메서드
    Optional<User> findByDiscordId(String discordId);
}
