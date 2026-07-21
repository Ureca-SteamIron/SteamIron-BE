package norimaets.appbatchserver.dto;

import java.util.List;

/**
 * 할인 목록 한 번 수집한 결과.
 * complete: 스팀 total_count까지 빠짐없이 받았는지 여부.
 *   부분 수집(false)일 때 "할인 종료" 판정을 돌리면, 안 받아온 게임들을 전부
 *   종료로 오판해 정가로 밀어버리는 데이터 오염이 발생하므로 반드시 이 값으로 가드한다.
 */
public record DiscountSnapshot(
        List<SteamDiscountItem> items,
        int totalCount,
        boolean complete
) {
}
