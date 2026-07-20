package norimaets.appnotificationserver.config;

import norimaets.moduledomainmongo.document.DiscordDeliveryLog;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

@Configuration
public class MongoIndexConfig {

    @Bean
    ApplicationRunner mongoIndexInitializer(MongoTemplate mongoTemplate) {
        return args -> {
            IndexOperations indexes = mongoTemplate.indexOps(DiscordDeliveryLog.class);

            indexes.ensureIndex(
                    new Index()
                            .on("eventKey", Direction.ASC)
                            .unique()
                            .named("uk_discord_delivery_event_key")
            );

            indexes.ensureIndex(
                    new Index()
                            .on("userId", Direction.ASC)
                            .named("idx_discord_delivery_user_id")
            );

            indexes.ensureIndex(
                    new Index()
                            .on("status", Direction.ASC)
                            .named("idx_discord_delivery_status")
            );
        };
    }
}