package norimaets.appapiserver.dto.response;

import java.time.LocalDateTime;
import norimaets.moduledomainrdb.entity.PriceHistory;

/**
 * 게임 상세 가격 변동 차트용 응답.
 * FE(PriceHistoryChart)의 toChartData가 price/discountPercent/recordedAt을 그대로 읽으므로 필드명을 맞춘다.
 */
public record PriceHistoryResponse(
        Integer price,
        Integer discountPercent,
        LocalDateTime recordedAt
) {
    public static PriceHistoryResponse from(PriceHistory history) {
        return new PriceHistoryResponse(
                history.getPrice(),
                history.getDiscountPercent(),
                history.getRecordedAt()
        );
    }
}
