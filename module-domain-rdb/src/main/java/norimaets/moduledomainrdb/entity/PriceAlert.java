package norimaets.moduledomainrdb.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "price_alert")
public class PriceAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "target_price", nullable = false)
    private Integer targetPrice;

    @Column(name = "is_active", nullable = false, columnDefinition = "boolean default true")
    private Boolean isActive;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_notified_price")
    private Integer lastNotifiedPrice;

    @Column(name = "last_notified_at")
    private LocalDateTime lastNotifiedAt;

    @Builder
    public PriceAlert(User user, Game game, Integer targetPrice, Boolean isActive) {
        this.user = user;
        this.game = game;
        this.targetPrice = targetPrice;
        this.isActive = isActive != null ? isActive : true;
    }

    // 알림 발송 후 갱신용 메서드
    public void updateLastNotified(Integer price, LocalDateTime notifiedAt) {
        this.lastNotifiedPrice = price;
        this.lastNotifiedAt = notifiedAt;
    }
}
