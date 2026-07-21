package norimaets.appapiserver.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 가격 알림 생성/수정 요청.
 *
 * ── 알림 방식은 2가지다 ─────────────────────────────────────────
 *   ① RATE : "할인율 N% 이상 되면 알림"  → 유저가 할인율(%) 입력, 화면 옆에 목표가 계산 표시
 *   ② ANY  : "1%라도 할인하면 알림"       → 할인율 입력 없음, 할인 시작하면 바로
 *
 *   두 방식 모두 서비스에서 결국 targetPrice(목표가) 하나로 변환해 저장한다.
 *   그래야 배치 판정(현재가 ≤ 목표가)이 방식과 무관하게 똑같이 동작한다.
 *     ① RATE → targetPrice = 정가 × (1 - rate/100)   (예: 정가 40000, rate 50 → 20000)
 *     ② ANY  → targetPrice = 정가 - 1                (정가보다 조금이라도 낮으면 걸림)
 *
 * TODO(현정님): 아래 record에 방식/할인율 필드를 추가한다.
 *   String  alertType;     // "RATE" | "ANY"   (enum으로 빼도 좋음)
 *   Integer discountRate;  // RATE일 때만 사용 (0~100). ANY면 null 허용
 *   ⚠ discountRate는 RATE에서만 필수 → 필드에 @NotNull 직접 붙이면 ANY 요청이 막힌다.
 *      "RATE면 discountRate 필수" 조건 검증은 서비스에서 처리 권장.
 *   그리고 서비스 create/updateTargetPrice 에서 위 공식으로 targetPrice를 계산해 넘기면 끝.
 *
 * 지금은 목표가를 직접 받는 최소 형태(틀)다. 위 필드만 붙이면 완성.
 */
public record PriceAlertRequest(
        @NotNull(message = "목표가를 입력해주세요.")
        @Positive(message = "목표가는 0보다 커야 합니다.")
        Integer targetPrice
) {
}
