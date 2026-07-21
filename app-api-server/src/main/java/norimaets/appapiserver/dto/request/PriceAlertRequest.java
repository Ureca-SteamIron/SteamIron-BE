package norimaets.appapiserver.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

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
 */
public record PriceAlertRequest(
        @NotNull(message = "알림 방식을 선택해주세요.")
        PriceAlertType alertType,

        @Min(value = 1, message = "할인율은 1 이상이어야 합니다.")
        @Max(value = 100, message = "할인율은 100 이하여야 합니다.")
        Integer discountRate
) {
}
