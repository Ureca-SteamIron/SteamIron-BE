package norimaets.appapiserver.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 할인 시작 알림과 지정 할인율 알림은 서로 독립적이며 둘 다 동시에 활성화할 수 있다.
 */
public record PriceAlertRequest(
        @NotNull(message = "할인 시작 알림 설정 여부를 입력해주세요.")
        Boolean discountStartEnabled,

        @NotNull(message = "지정 할인율 알림 설정 여부를 입력해주세요.")
        Boolean targetDiscountEnabled,

        @Min(value = 1, message = "할인율은 1 이상이어야 합니다.")
        @Max(value = 100, message = "할인율은 100 이하여야 합니다.")
        Integer discountRate
) {
    @AssertTrue(message = "할인 시작 알림 또는 지정 할인율 알림 중 하나 이상을 선택해주세요.")
    public boolean isAtLeastOneAlertEnabled() {
        return Boolean.TRUE.equals(discountStartEnabled)
                || Boolean.TRUE.equals(targetDiscountEnabled);
    }

    @AssertTrue(message = "지정 할인율 알림을 사용하려면 할인율을 입력해주세요.")
    public boolean isTargetDiscountConfigurationValid() {
        return !Boolean.TRUE.equals(targetDiscountEnabled) || discountRate != null;
    }
}
