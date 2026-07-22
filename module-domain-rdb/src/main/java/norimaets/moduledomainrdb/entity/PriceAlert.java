package norimaets.moduledomainrdb.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

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

    @Column(name = "discount_rate")
    private Integer discountRate;

    @Column(name = "discount_start_enabled")
    private Boolean discountStartEnabled;

    @Column(name = "target_discount_enabled")
    private Boolean targetDiscountEnabled;

    @Column(name = "is_active", nullable = false, columnDefinition = "boolean default true")
    private Boolean isActive;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_notified_price")
    private Integer lastNotifiedPrice;

    @Column(name = "last_notified_at")
    private LocalDateTime lastNotifiedAt;

    @Column(name = "last_discount_start_notified_price")
    private Integer lastDiscountStartNotifiedPrice;

    @Column(name = "last_discount_start_notified_at")
    private LocalDateTime lastDiscountStartNotifiedAt;

    @Builder
    public PriceAlert(
            User user,
            Game game,
            Integer targetPrice,
            Integer discountRate,
            Boolean discountStartEnabled,
            Boolean targetDiscountEnabled,
            Boolean isActive
    ) {
        this.user = user;
        this.game = game;
        this.targetPrice = targetPrice;
        this.discountRate = discountRate;
        this.discountStartEnabled = Boolean.TRUE.equals(discountStartEnabled);
        this.targetDiscountEnabled = Boolean.TRUE.equals(targetDiscountEnabled);
        this.isActive = isActive != null ? isActive : true;
    }

    // 알림 발송 후 갱신용 메서드
    public void updateLastNotified(Integer price, LocalDateTime notifiedAt) {
        this.lastNotifiedPrice = price;
        this.lastNotifiedAt = notifiedAt;
    }

    public void updateDiscountStartLastNotified(Integer price, LocalDateTime notifiedAt) {
        this.lastDiscountStartNotifiedPrice = price;
        this.lastDiscountStartNotifiedAt = notifiedAt;
    }

    // 목표가 변경 (단순 세팅. 할인율→목표가 계산은 서비스/컨트롤러 레이어에서 처리)
    public void updateTargetPrice(Integer targetPrice) {
        this.targetPrice = targetPrice;
    }

    public void updateSettings(
            Integer targetPrice,
            Integer discountRate,
            boolean discountStartEnabled,
            boolean targetDiscountEnabled
    ) {
        boolean discountStartNewlyEnabled = !isDiscountStartEnabled() && discountStartEnabled;
        boolean targetChanged = !Objects.equals(this.targetPrice, targetPrice)
                || !isTargetDiscountEnabled() && targetDiscountEnabled;

        this.targetPrice = targetPrice;
        this.discountRate = discountRate;
        this.discountStartEnabled = discountStartEnabled;
        this.targetDiscountEnabled = targetDiscountEnabled;

        if (discountStartNewlyEnabled) {
            this.lastDiscountStartNotifiedPrice = null;
            this.lastDiscountStartNotifiedAt = null;
        }
        if (targetChanged) {
            this.lastNotifiedPrice = null;
            this.lastNotifiedAt = null;
        }
    }

    public boolean isDiscountStartEnabled() {
        if (discountStartEnabled != null) {
            return discountStartEnabled;
        }
        return isLegacyAnyDiscountAlert();
    }

    public boolean isTargetDiscountEnabled() {
        if (targetDiscountEnabled != null) {
            return targetDiscountEnabled;
        }
        return !isLegacyAnyDiscountAlert();
    }

    public Integer resolveDiscountRate() {
        if (discountRate != null) {
            return discountRate;
        }
        if (!isTargetDiscountEnabled() || game == null || game.getOriginalPrice() == null
                || game.getOriginalPrice() <= 0 || targetPrice == null) {
            return null;
        }

        int originalPrice = game.getOriginalPrice();
        for (int rate = 1; rate <= 100; rate++) {
            int calculated = (int) ((long) originalPrice * (100 - rate) / 100);
            if (calculated == targetPrice) {
                return rate;
            }
        }
        return Math.max(1, Math.min(100,
                Math.round((originalPrice - targetPrice) * 100f / originalPrice)));
    }

    private boolean isLegacyAnyDiscountAlert() {
        return discountStartEnabled == null
                && targetDiscountEnabled == null
                && game != null
                && game.getOriginalPrice() != null
                && targetPrice != null
                && targetPrice.equals(game.getOriginalPrice() - 1);
    }

    // 알림 켜기/끄기 (게임별)
    public void changeActive(boolean active) {
        this.isActive = active;
    }
}
