package norimaets.appnotificationserver;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import norimaets.moduledomainmongo.document.DiscordDeliveryLog;
import norimaets.moduledomainmongo.document.DiscordDeliveryStatus;
import norimaets.moduledomainmongo.repository.DiscordDeliveryLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "MONGODB_URI", matches = ".+")
class MongoConnectionIntegrationTest {

    @Autowired
    private DiscordDeliveryLogRepository repository;

    @Test
    void savesAndReadsDiscordDeliveryLog() {
        String eventKey = "connection-test-" + UUID.randomUUID();

        DiscordDeliveryLog log = DiscordDeliveryLog.builder()
                .eventKey(eventKey)
                .userId(1L)
                .discordUserId("integration-test-user")
                .gameId(1L)
                .gameName("integration-test-game")
                .targetPrice(15000)
                .currentPrice(12000)
                .discountPercent(20)
                .status(DiscordDeliveryStatus.PENDING)
                .build();

        DiscordDeliveryLog saved = repository.save(log);

        try {
            DiscordDeliveryLog found = repository.findByEventKey(eventKey)
                    .orElseThrow();

            assertThat(found.getId()).isEqualTo(saved.getId());
            assertThat(found.getStatus()).isEqualTo(DiscordDeliveryStatus.PENDING);
        } finally {
            repository.deleteById(saved.getId());
        }
    }
}