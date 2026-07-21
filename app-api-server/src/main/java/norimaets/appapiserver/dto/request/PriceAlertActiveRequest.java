package norimaets.appapiserver.dto.request;

import jakarta.validation.constraints.NotNull;

public record PriceAlertActiveRequest(
        @NotNull(message = "알림 활성화 여부를 입력해주세요.")
        Boolean active
) {
}
