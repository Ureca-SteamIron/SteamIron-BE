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

        // 가격 미수집(데모 등) 게임 제외: 무료가 아닌데 가격 정보가 아예 없는 경우
        spec = spec.and((root, query, builder) -> builder.or(
                builder.isTrue(root.get("isFree")),
                builder.and(
                        builder.isNotNull(root.get("originalPrice")),
                        builder.isNotNull(root.get("finalPrice"))
                )
        ));

        return spec;
    }

    public static Sort resolveSort(String sortType) {
        // finalPrice/discountPercent는 동점(같은 값)인 행이 아주 많아서, 이 컬럼만으로 정렬하면
        // 페이지(LIMIT/OFFSET)마다 동점 그룹 내부 순서가 달라질 수 있다. 그러면 어떤 게임은
        // 두 페이지에 걸쳐 중복으로 나오고 어떤 게임은 누락된다. id를 2차 정렬 키로 붙여
        // 정렬 결과를 항상 결정적으로 만든다(이름순 정렬과 동일한 처리).
        Sort idTieBreaker = Sort.by(Sort.Direction.ASC, "id");
        switch (sortType != null ? sortType : "popular") {
            case "price_asc":
                return Sort.by(new Sort.Order(Sort.Direction.ASC, "finalPrice", Sort.NullHandling.NULLS_FIRST))
                        .and(idTieBreaker);
            case "price_desc":
                return Sort.by(new Sort.Order(Sort.Direction.DESC, "finalPrice", Sort.NullHandling.NULLS_LAST))
                        .and(idTieBreaker);
            case "discount_desc":
                return Sort.by(Sort.Direction.DESC, "discountPercent").and(idTieBreaker);
            case "name_asc":
                return Sort.by(Sort.Direction.ASC, "name");
            case "name_desc":
                return Sort.by(Sort.Direction.DESC, "name");
            default:
                return Sort.by(Sort.Direction.ASC, "id");
        }
    }
}