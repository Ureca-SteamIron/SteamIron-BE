package norimaets.appapiserver.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GameFilterRequest {
    private String genre = "all";
    private String priceType = "all";
    private Integer minPrice;
    private Integer maxPrice;
    private Integer minDiscount = 0;
    private boolean sale = false;
    private String sort = "popular";
}
