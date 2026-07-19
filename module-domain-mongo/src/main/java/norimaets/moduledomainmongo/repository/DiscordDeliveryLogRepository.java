package norimaets.moduledomainmongo.repository;

import java.util.List;
import java.util.Optional;
import norimaets.moduledomainmongo.document.DiscordDeliveryLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DiscordDeliveryLogRepository
        extends MongoRepository<DiscordDeliveryLog, String> {

    Optional<DiscordDeliveryLog> findByEventKey(String eventKey);

    List<DiscordDeliveryLog> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}