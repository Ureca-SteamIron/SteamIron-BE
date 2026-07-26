package norimaets.appnotificationserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
@EnableMongoRepositories(basePackages = "norimaets.moduledomainmongo.repository")
public class AppNotificationServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppNotificationServerApplication.class, args);
    }
}