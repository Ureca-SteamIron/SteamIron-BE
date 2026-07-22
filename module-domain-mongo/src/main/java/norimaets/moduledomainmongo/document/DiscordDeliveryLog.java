package norimaets.moduledomainmongo.document;

import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Document(collection = "discord_delivery_logs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DiscordDeliveryLog {

    @Id
    private String id;

    @Indexed(name = "uk_discord_delivery_event_key", unique = true)
    private String eventKey;
    private String notificationType;

    @Indexed(name = "idx_discord_delivery_user_id")
    private Long userId;

    private String discordUserId;
    private Long gameId;
    private String gameName;

    private Integer targetPrice;
    private Integer currentPrice;
    private Integer discountPercent;

    @Indexed(name = "idx_discord_delivery_status")
    private DiscordDeliveryStatus status;

    private Integer attemptCount;
    private String discordMessageId;
    private String errorCode;
    private String errorMessage;

    private Instant createdAt;
    private Instant sentAt;

    @Builder
    public DiscordDeliveryLog(
            String eventKey,
            String notificationType,
            Long userId,
            String discordUserId,
            Long gameId,
            String gameName,
            Integer targetPrice,
            Integer currentPrice,
            Integer discountPercent,
            DiscordDeliveryStatus status,
            Integer attemptCount,
            String discordMessageId,
            String errorCode,
            String errorMessage,
            Instant createdAt,
            Instant sentAt
    ) {
        this.eventKey = eventKey;
        this.notificationType = notificationType;
        this.userId = userId;
        this.discordUserId = discordUserId;
        this.gameId = gameId;
        this.gameName = gameName;
        this.targetPrice = targetPrice;
        this.currentPrice = currentPrice;
        this.discountPercent = discountPercent;
        this.status = status != null ? status : DiscordDeliveryStatus.PENDING;
        this.attemptCount = attemptCount != null ? attemptCount : 0;
        this.discordMessageId = discordMessageId;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.sentAt = sentAt;
    }

    // 발송 성공 시 상태 갱신
    public void markSent(String discordMessageId) {
        this.status = DiscordDeliveryStatus.SENT;
        this.discordMessageId = discordMessageId;
        this.sentAt = Instant.now();
        this.attemptCount = (attemptCount == null ? 0 : attemptCount) + 1;
        this.errorCode = null;
        this.errorMessage = null;
    }

    // 발송 실패 시 상태 갱신 (배치가 다음 주기에 재시도)
    public void markFailed(String errorCode, String errorMessage) {
        this.status = DiscordDeliveryStatus.FAILED;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.attemptCount = (attemptCount == null ? 0 : attemptCount) + 1;
    }
}
