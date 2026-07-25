package norimaets.appapiserver.dto.response;

import lombok.Builder;
import lombok.Getter;
import norimaets.moduledomainrdb.entity.Game;
import norimaets.moduledomainrdb.entity.GameGenre;
import norimaets.moduledomainrdb.entity.Genre;

import java.util.List;
import java.util.stream.Collectors;

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
    private List<String> genres;

    // boolean(원시형)으로 두면 Lombok이 isWishlisted() 게터를 만들고 Jackson이 "is"를 떼서
    // "wishlisted"로 내려버려 프론트가 기대하는 필드명과 어긋난다. Boolean(래퍼)로 두면
    // getIsWishlisted() 게터가 생성되어 isFree와 동일하게 "isWishlisted"로 정상 직렬화된다.
    private Boolean isWishlisted;
    private Long wishlistCount;

    private Integer aiScore;

    /**
     * Entity 조각들을 모아서 하나의 완전한 DTO로 조립하는 팩토리 메서드
     * AI 요약은 별도 엔드포인트(GET /api/games/{appId}/ai-summary)로 분리했으므로 여기서는 다루지 않는다.
     */
    public static GameDetailResponse of(Game game, boolean isWishlisted, long wishlistCount) {
        return GameDetailResponse.builder()
                .appId(game.getId())
                .name(game.getName())
                .headerImage(game.getHeaderImage())
                .originalPrice(game.getOriginalPrice())
                .finalPrice(game.getFinalPrice())
                .discountPercent(game.getDiscountPercent())
                .isFree(game.getIsFree())
                .genres(game.getGameGenres().stream()
                        .map(GameGenre::getGenre)
                        .map(Genre::getName)
                        .collect(Collectors.toList()))

                .isWishlisted(isWishlisted)
                .wishlistCount(wishlistCount)

                .build();
    }
}
