package norimaets.appapiserver.specification;

import jakarta.persistence.criteria.Join;
import norimaets.appapiserver.dto.request.GameFilterRequest;
import norimaets.moduledomainrdb.entity.Game;
import norimaets.moduledomainrdb.entity.GameGenre;
import norimaets.moduledomainrdb.entity.Genre;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

public class GameSpecs {

    public static Specification<Game> baseFilter(GameFilterRequest req) {
        Specification<Game> spec = Specification.unrestricted();

        if (req.getGenre() != null && !"all".equalsIgnoreCase(req.getGenre())) {
            spec = spec.and((root, query, builder) -> {
                Join<Game, GameGenre> gameGenreJoin = root.join("gameGenres");
                Join<GameGenre, Genre> genreJoin = gameGenreJoin.join("genre");
                return builder.equal(genreJoin.get("name"), req.getGenre());
            });
        }

        if ("free".equalsIgnoreCase(req.getPriceType())) {
            spec = spec.and((root, query, builder) -> builder.isTrue(root.get("isFree")));
        } else if ("paid".equalsIgnoreCase(req.getPriceType())) {
            spec = spec.and((root, query, builder) -> builder.isFalse(root.get("isFree")));
        }

        if (req.getMinPrice() != null) {
            spec = spec.and((root, query, builder) -> builder.greaterThanOrEqualTo(root.get("finalPrice"), req.getMinPrice()));
        }
        if (req.getMaxPrice() != null) {
            spec = spec.and((root, query, builder) -> builder.lessThanOrEqualTo(root.get("finalPrice"), req.getMaxPrice()));
        }
        if (req.getMinDiscount() != null && req.getMinDiscount() > 0) {
            spec = spec.and((root, query, builder) -> builder.greaterThanOrEqualTo(root.get("discountPercent"), req.getMinDiscount()));
        }
        if (req.isSale()) {
            spec = spec.and((root, query, builder) -> builder.greaterThan(root.get("discountPercent"), 0));
        }

        return spec;
    }

    public static Sort resolveSort(String sortType) {
        switch (sortType != null ? sortType : "popular") {
            case "price_asc": return Sort.by(Sort.Direction.ASC, "finalPrice");
            case "price_desc": return Sort.by(Sort.Direction.DESC, "finalPrice");
            case "discount_desc": return Sort.by(Sort.Direction.DESC, "discountPercent");
            case "name_asc": return Sort.by(Sort.Direction.ASC, "name");
            case "name_desc": return Sort.by(Sort.Direction.DESC, "name");
            default: return Sort.by(Sort.Direction.ASC, "id");
        }
    }
}