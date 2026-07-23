package norimaets.appapiserver.dto.response;

import lombok.Builder;
import lombok.Getter;
import norimaets.appapiserver.service.GeminiService;
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
    private String description;

    private boolean isWishlisted;

    private Integer aiScore;
    private String aiExplanation;

    /**
     * Entity 조각들을 모아서 하나의 완전한 DTO로 조립하는 팩토리 메서드
     */
    public static GameDetailResponse of(Game game,
                                        String aiExplanation,
                                        boolean isWishlisted) {
        return GameDetailResponse.builder()
                .appId(game.getId())
                .name(game.getName())
                .headerImage(game.getHeaderImage())
                .originalPrice(game.getOriginalPrice())
                .finalPrice(game.getFinalPrice())
                .discountPercent(game.getDiscountPercent())

                .isWishlisted(isWishlisted)

                // 💡 주석을 해제하고 Gemini가 생성한 요약 텍스트를 담아줍니다.
                .aiExplanation(aiExplanation != null ? aiExplanation : "AI 요약을 불러오는 중입니다.")
                .build();
    }
}
