package norimaets.appapiserver.dto.response;

import lombok.Builder;
import lombok.Getter;
import norimaets.moduledomainrdb.entity.Game;

@Getter
@Builder
public class GameDetailResponse {
    private Long appId;
    private String name;
    private String headerImage;
    private Integer originalPrice;
    private Integer finalPrice;
    private Integer discountPercent;
    private Boolean isFree;
    private String description;

    private boolean isWishlisted;

    private Integer aiScore;

    /**
     * Entity 조각들을 모아서 하나의 완전한 DTO로 조립하는 팩토리 메서드
     * AI 요약은 별도 엔드포인트(GET /api/games/{appId}/ai-summary)로 분리했으므로 여기서는 다루지 않는다.
     */
    public static GameDetailResponse of(Game game, boolean isWishlisted) {
        return GameDetailResponse.builder()
                .appId(game.getId())
                .name(game.getName())
                .headerImage(game.getHeaderImage())
                .originalPrice(game.getOriginalPrice())
                .finalPrice(game.getFinalPrice())
                .discountPercent(game.getDiscountPercent())
                .isFree(game.getIsFree())

                .isWishlisted(isWishlisted)

                .build();
    }
}
