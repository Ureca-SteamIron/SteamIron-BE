package norimaets.appapiserver.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 이 프로젝트엔 아직 Flyway/Liquibase 같은 마이그레이션 도구가 없고, application.yml도
 * .gitignore 대상이라 설정 파일로는 팀 전체에 퍼지지 않는다. 그래서 게임 검색(pg_trgm)에
 * 필요한 DB 확장 설치를 코드로 보장한다 — 앱이 뜰 때마다 한 번씩 실행되며,
 * CREATE EXTENSION IF NOT EXISTS는 멱등이라 이미 설치돼 있으면 아무 일도 안 하고 넘어간다.
 * (이게 없으면 pg_trgm이 설치 안 된 DB에서 GameRepository.searchByNameSimilarity()가
 * "function word_similarity(...) does not exist" 에러로 500을 낸다.)
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class DatabaseExtensionInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");
        log.info("pg_trgm extension ensured.");
    }
}
