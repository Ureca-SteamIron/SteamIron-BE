package norimaets.appapiserver.common.sort;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;
import norimaets.moduledomainrdb.entity.Game;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 게임 이름 정렬 정책을 한 곳에 모은 유틸.
 *
 * DB 콜레이션(korean, ICU ko-KR)만으로는 "한글을 영어보다 먼저" 같은 그룹 우선순위를 표현할 수 없어서,
 * "첫 글자가 어느 문자 그룹인지"를 1차 정렬 키로 쓰고, 그 안에서 이름순으로 2차 정렬한다.
 *
 * 정책:
 *  - 오름차순(name_asc): 한글 → 영어 → 기타,  각 그룹 내부는 이름 오름차순
 *  - 내림차순(name_desc): 영어 → 한글 → 기타, 각 그룹 내부는 이름 내림차순
 *
 * 이름 자체의 가나다/알파벳 순서는 game.name 컬럼에 걸린 korean 콜레이션이 담당한다
 * (ALTER TABLE game ALTER COLUMN name TYPE VARCHAR(255) COLLATE korean 적용됨).
 * 그래서 여기서는 그룹 순서(CASE)만 얹으면 되고, 이름 비교(ORDER BY name)는 DB가 컬럼 콜레이션으로 처리한다.
 *
 * 메인 화면(getFilteredTop100Games)은 최대 100개라 자바 Comparator로 메모리 정렬하고,
 * 검색(searchGames)은 페이지네이션 때문에 DB에서 정렬해야 하므로 Criteria용 Order 리스트를 만들어 쓴다.
 * 두 경로가 같은 그룹 규칙을 공유하도록 그룹 판정 기준을 이 클래스 한 곳에 둔다.
 *
 * 그룹 판정은 DB/자바 양쪽에서 "첫 글자의 유니코드 코드포인트"로 통일한다.
 *  - DB:   PostgreSQL ascii(text)는 첫 문자의 유니코드 코드포인트를 반환한다(PG 14 지원).
 *  - 자바: name.charAt(0). 한글 완성형은 BMP라 char 하나로 코드포인트가 그대로 나온다.
 */
public final class GameNameSort {

    private GameNameSort() {
    }

    // 한글 완성형 음절 블록: 가(U+AC00=44032) ~ 힣(U+D7A3=55203)
    private static final int HANGUL_SYLLABLE_START = 0xAC00; // 44032
    private static final int HANGUL_SYLLABLE_END = 0xD7A3;   // 55203

    private static final int GROUP_HANGUL = 0;
    private static final int GROUP_LATIN = 1;
    private static final int GROUP_OTHER = 2;

    /** 오름차순 그룹 순위: 한글(0) → 영어(1) → 기타(2). 이름이 비어 있으면 기타로 취급. */
    private static int ascGroup(String name) {
        if (name == null || name.isEmpty()) return GROUP_OTHER;
        char c = name.charAt(0);
        if (c >= HANGUL_SYLLABLE_START && c <= HANGUL_SYLLABLE_END) return GROUP_HANGUL;
        if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z')) return GROUP_LATIN;
        return GROUP_OTHER;
    }

    /** 내림차순 그룹 순위: 영어(0) → 한글(1) → 기타(2). */
    private static int descGroupRank(String name) {
        int g = ascGroup(name);
        if (g == GROUP_HANGUL) return 1;
        if (g == GROUP_LATIN) return 0;
        return GROUP_OTHER;
    }

    // ---------------------- 메인 화면(메모리 정렬)용 Comparator ----------------------

    public static Comparator<Game> ascComparator() {
        return Comparator
                .comparingInt((Game g) -> ascGroup(g.getName()))
                .thenComparing(g -> nullSafe(g.getName()));
    }

    public static Comparator<Game> descComparator() {
        // 그룹은 desc 정책 순서로, 이름은 그룹 내부 역순.
        return Comparator
                .comparingInt((Game g) -> descGroupRank(g.getName()))
                .thenComparing(Comparator.comparing((Game g) -> nullSafe(g.getName())).reversed());
    }

    // ---------------------- 검색(페이지네이션)용 Criteria Order ----------------------

    /**
     * query.orderBy(...)에 그대로 넣을 Order 리스트.
     * 이름 컬럼 자체가 korean 콜레이션이라 name asc/desc가 곧 한국어 정렬이다.
     * 호출부에서 id 타이브레이커를 뒤에 덧붙여 페이지네이션 결과를 결정적으로 유지한다.
     *
     * @param desc true면 내림차순 정책(영어→한글→기타 + 이름 역순)
     */
    public static List<Order> criteriaOrders(CriteriaBuilder cb, Root<Game> root, boolean desc) {
        Expression<String> name = root.get("name");
        Expression<Integer> groupExpr = groupCase(cb, name, desc);

        if (desc) {
            return List.of(cb.asc(groupExpr), cb.desc(name));
        }
        return List.of(cb.asc(groupExpr), cb.asc(name));
    }

    /** 정렬 타입이 이름순(name_asc/name_desc)인지. */
    public static boolean isNameSort(String sortType) {
        return "name_asc".equals(sortType) || "name_desc".equals(sortType);
    }

    /**
     * 페이지네이션 있는 조회에서 이름순 그룹 정렬을 걸기 위한 Specification 조각.
     * query.orderBy에 CASE(그룹) + name(콜레이션) + id(타이브레이커)를 심는다.
     * 이걸 baseFilter Specification에 .and()로 붙이고, Pageable에는 정렬을 싣지 않는다.
     * count 쿼리(resultType=Long)에는 orderBy를 걸지 않는다 (불필요 + 일부 DB에서 오류).
     *
     * @param desc true면 name_desc(영어→한글→기타 + 이름 역순), false면 name_asc
     */
    public static Specification<Game> orderSpec(boolean desc) {
        return (root, query, builder) -> {
            if (query != null && Long.class != query.getResultType() && long.class != query.getResultType()) {
                List<Order> orders = new ArrayList<>(criteriaOrders(builder, root, desc));
                orders.add(builder.asc(root.get("id"))); // id 타이브레이커로 결정적 페이지네이션 보장
                query.orderBy(orders);
            }
            return builder.conjunction();
        };
    }

    /**
     * 첫 글자 코드포인트로 그룹 순위를 내는 SQL CASE 식.
     * PostgreSQL ascii(text) = 첫 문자의 유니코드 코드포인트(PG 14 지원).
     */
    private static Expression<Integer> groupCase(CriteriaBuilder cb, Expression<String> name, boolean desc) {
        Expression<Integer> firstCp = cb.function("ascii", Integer.class, name);

        Expression<Boolean> isHangul = cb.between(firstCp, HANGUL_SYLLABLE_START, HANGUL_SYLLABLE_END);
        Expression<Boolean> isUpper = cb.between(firstCp, (int) 'A', (int) 'Z');
        Expression<Boolean> isLower = cb.between(firstCp, (int) 'a', (int) 'z');
        Expression<Boolean> isLatin = cb.or(cb.isTrue(isUpper), cb.isTrue(isLower));

        int hangulRank = desc ? 1 : 0;
        int latinRank = desc ? 0 : 1;

        return cb.<Integer>selectCase()
                .when(cb.isTrue(isHangul), hangulRank)
                .when(isLatin, latinRank)
                .otherwise(GROUP_OTHER);
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }
}
