package norimaets.appbatchserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(
        scanBasePackages = {
                "norimaets.appbatchserver",    // 내 구역 스캔
                "norimaets.moduledomainrdb"  // ⭐️ DB 모듈 구역(Service, Component 등) 스캔
        }
)
@EntityScan(basePackages = "norimaets.moduledomainrdb")
@EnableJpaRepositories(basePackages = "norimaets.moduledomainrdb")
@EnableScheduling
public class AppBatchServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppBatchServerApplication.class, args);
    }

}
