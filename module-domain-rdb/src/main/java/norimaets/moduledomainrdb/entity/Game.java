package norimaets.moduledomainrdb.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "game")
public class Game {

    @Id
    @Column(name = "game_id")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "header_image", length = 500)
    private String headerImage;

    @Column(name = "original_price")
    private Integer originalPrice;

    @Column(name = "final_price")
    private Integer finalPrice;

    @Column(name = "discount_percent", columnDefinition = "integer default 0")
    private Integer discountPercent;

    @Column(name = "is_free", nullable = false, columnDefinition = "boolean default false")
    private Boolean isFree;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GameGenre> gameGenres = new ArrayList<>();

    @Builder
    public Game(Long id, String name, String headerImage, Integer originalPrice, Integer finalPrice, Integer discountPercent, Boolean isFree) {
        this.id = id;
        this.name = name;
        this.headerImage = headerImage;
        this.originalPrice = originalPrice;
        this.finalPrice = finalPrice;
        this.discountPercent = discountPercent != null ? discountPercent : 0;
        this.isFree = isFree != null ? isFree : false;
    }

    // 배치 서버에서 가격 업데이트 시 사용할 비즈니스 메서드
    public void updatePriceInfo(Integer originalPrice, Integer finalPrice, Integer discountPercent, Boolean isFree) {
        this.originalPrice = originalPrice;
        this.finalPrice = finalPrice;
        this.discountPercent = discountPercent;
        this.isFree = isFree;
    }

    public void updateIfPresent(String name, String headerImage, Integer originalPrice,
                                Integer finalPrice, Integer discountPercent, Boolean isFree) {
        if (name != null) this.name = name;
        if (headerImage != null) this.headerImage = headerImage;
        if (originalPrice != null) this.originalPrice = originalPrice;
        if (finalPrice != null) this.finalPrice = finalPrice;
        if (discountPercent != null) this.discountPercent = discountPercent;
        if (isFree != null) this.isFree = isFree;
    }
}
