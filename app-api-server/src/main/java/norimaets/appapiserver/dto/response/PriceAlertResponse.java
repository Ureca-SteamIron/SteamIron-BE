package norimaets.appapiserver.dto.response;

import norimaets.moduledomainrdb.entity.Game;
import norimaets.moduledomainrdb.entity.PriceAlert;

/**
 * 내 가격 알림 목록 응답 한 건.
 * 목표가(targetPrice)와 함께 게임의 현재가(finalPrice)를 같이 내려줘 화면에서 비교 표시할 수 있게 한다.
 */
public record PriceAlertResponse(
        Long alertId,
        Long gameId,
        String gameName,
        String headerImage,
        Integer targetPrice,
        Integer currentPrice,   // 게임의 현재 최종가 (도달 여부 판단/표시용)
        Boolean isActive
) {
    public static PriceAlertResponse from(PriceAlert alert) {
        Game game = alert.getGame();
        return new PriceAlertResponse(
                alert.getId(),
                game.getId(),
                game.getName(),
                game.getHeaderImage(),
                alert.getTargetPrice(),
                game.getFinalPrice(),
                alert.getIsActive()
        );
    }
}
