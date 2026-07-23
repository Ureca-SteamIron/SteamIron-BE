package norimaets.moduledomainmongo.document;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Document(collection = "user_notifications")
@CompoundIndex(
        name = "idx_user_notification_user_created",
        def = "{'userId': 1, 'createdAt': -1}"
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserNotification {

    @Id
    private String id;

    @Indexed(
            name = "uk_user_notification_event_key",
            unique = true
    )
    private String eventKey;

    private UserNotificationType notificationType;

    private Long alertId;
    private Long userId;

    private Long gameId;
    private String gameName;

    private Integer targetPrice;
    private Integer currentPrice;
    private Integer discountPercent;

    private boolean read;
    private Instant createdAt;
    private Instant readAt;

    @Builder
    public UserNotification(
            String eventKey,
            UserNotificationType notificationType,
            Long alertId,
            Long userId,
            Long gameId,
            String gameName,
            Integer targetPrice,
            Integer currentPrice,
            Integer discountPercent,
            Boolean read,
            Instant createdAt,
            Instant readAt
    ) {
        this.eventKey = eventKey;
        this.notificationType = notificationType;
        this.alertId = alertId;
        this.userId = userId;
        this.gameId = gameId;
        this.gameName = gameName;
        this.targetPrice = targetPrice;
        this.currentPrice = currentPrice;
        this.discountPercent = discountPercent;
        this.read = read != null && read;
        this.createdAt = createdAt != null
                ? createdAt
                : Instant.now();
        this.readAt = readAt;
    }

    public void markRead() {
        if (read) {
            return;
        }

        this.read = true;
        this.readAt = Instant.now();
    }
}