package norimaets.appapiserver.dto.response;

import lombok.Builder;
import lombok.Getter;
import norimaets.moduledomainrdb.entity.Game;
import norimaets.moduledomainrdb.entity.GameGenre;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class GameSimpleResponse {

    private Long appId;
    private String name;
    private String headerImage;
    private Integer originalPrice;
    private Integer finalPrice;
    private Integer discountPercent;
    private Boolean isFree;
    private List<String> genres;

    /**
     * Game 엔티티를 GameSimpleResponse DTO로 변환하는 정적 팩토리 메서드
     */
    public static GameSimpleResponse from(Game game) {
        return GameSimpleResponse.builder()
                .appId(game.getId())
                .name(game.getName())
                .headerImage(game.getHeaderImage())
                .originalPrice(game.getOriginalPrice())
                .finalPrice(game.getFinalPrice())
                .discountPercent(game.getDiscountPercent())
                .isFree(game.getIsFree())
                .genres(game.getGameGenres().stream()
                        .map(GameGenre::getGenre)
                        .map(norimaets.moduledomainrdb.entity.Genre::getName)
                        .collect(Collectors.toList()))
                .build();
    }
}