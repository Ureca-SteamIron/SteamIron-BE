package norimaets.appbatchserver.dto;

/**
 * 스팀 할인 목록 HTML에서 한 게임을 파싱한 결과.
 * originalPrice: 정가(취소선). 파싱 실패 시 null일 수 있어 정가 복원은 DB 값을 우선 사용한다.
 * finalPrice: 현재 할인가. discountPercent: 할인율(%).
 */
public record SteamDiscountItem(
        Long appId,
        String name,
        Integer originalPrice,
        Integer finalPrice,
        Integer discountPercent
) {
}
