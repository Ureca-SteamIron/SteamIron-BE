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
    private String description;

    private boolean isWishlisted;

    private Integer aiScore;
    private String aiExplanation;

    /**
     * Entity 조각들을 모아서 하나의 완전한 DTO로 조립하는 팩토리 메서드
     */
    public static GameDetailResponse of(Game game,
//                                        GameAiAnalysis aiAnalysis,
                                        boolean isWishlisted) {
        return GameDetailResponse.builder()
                .appId(game.getId())
                .name(game.getName())
                .headerImage(game.getHeaderImage())
                .originalPrice(game.getOriginalPrice())
                .finalPrice(game.getFinalPrice())
                .discountPercent(game.getDiscountPercent())
                // .description(game.getDescription()) // 필요시 Game 엔티티에 추가

                .isWishlisted(isWishlisted) // 외부에서 계산된 결과 주입

                // AI 분석 결과가 아직 없을 경우(null)를 대비한 안전한 매핑
//                .aiScore(aiAnalysis != null ? aiAnalysis.getScore() : null)
//                .aiExplanation(aiAnalysis != null ? aiAnalysis.getExplanation() : "AI 분석이 진행 중입니다.")
                .build();
    }
}
