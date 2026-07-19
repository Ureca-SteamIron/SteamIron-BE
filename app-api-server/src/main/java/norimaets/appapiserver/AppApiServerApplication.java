package norimaets.appapiserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(
        scanBasePackages = {
                "norimaets.appapiserver",
                "norimaets.moduledomainrdb"
        }
)
@EntityScan(basePackages = "norimaets.moduledomainrdb")
@EnableJpaRepositories(basePackages = "norimaets.moduledomainrdb")
public class AppApiServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppApiServerApplication.class, args);
    }

}

// deploy: Java 21 적용 확인용 무해한 변경 (2026-07-19)
