package norimaets.appapiserver.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import norimaets.appapiserver.common.exception.CustomException;
import norimaets.appapiserver.common.exception.ErrorCode;
import norimaets.appapiserver.dto.request.PriceAlertRequest;
import norimaets.appapiserver.dto.response.PriceAlertResponse;
import norimaets.moduledomainrdb.entity.Game;
import norimaets.moduledomainrdb.entity.PriceAlert;
import norimaets.moduledomainrdb.entity.User;
import norimaets.moduledomainrdb.repository.GameRepository;
import norimaets.moduledomainrdb.repository.PriceAlertRepository;
import norimaets.moduledomainrdb.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PriceAlertService {

    private final PriceAlertRepository priceAlertRepository;
    private final GameRepository gameRepository;
    private final UserRepository userRepository;

    /**
     * 로그인한 사용자의 가격 알림 목록을 조회한다.
     */
    public List<PriceAlertResponse> getMyAlerts(Long userId) {
        return priceAlertRepository.findAllByUserIdWithGame(userId)
                .stream()
                .map(PriceAlertResponse::from)
                .toList();
    }

    /**
     * 게임 가격 알림을 생성한다.
     *
     * RATE:
     * 정가와 할인율을 이용하여 목표 가격을 계산한다.
     *
     * ANY:
     * 정가보다 1원이라도 낮아지면 알림이 발생하도록
     * 정가 - 1을 목표 가격으로 설정한다.
     */
    // 알림 생성 (같은 게임에 이미 있으면 중복 거부)
    @Transactional
    public void create(
            Long userId,
            Long gameId,
            PriceAlertRequest request
    ) {
        if (priceAlertRepository.existsByUser_IdAndGame_Id(userId, gameId)) {
            throw new CustomException(
                    ErrorCode.ALERT_ALREADY_EXISTS
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.USER_NOT_FOUND)
                );

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.GAME_NOT_FOUND)
                );

        int targetPrice = calculateTargetPrice(game, request);

        PriceAlert priceAlert = PriceAlert.builder()
                .user(user)
                .game(game)
                .targetPrice(targetPrice)
                .build();

        priceAlertRepository.save(priceAlert);
    }

    /**
     * 본인이 설정한 가격 알림의 목표 가격을 변경한다.
     */
    @Transactional
    public void updateTargetPrice(
            Long userId,
            Long alertId,
            PriceAlertRequest request
    ) {
        PriceAlert alert = findOwnedAlert(alertId, userId);

        int targetPrice = calculateTargetPrice(
                alert.getGame(),
                request
        );

        alert.updateTargetPrice(targetPrice);
    }

    /**
     * 본인이 설정한 가격 알림을 켜거나 끈다.
     */
    @Transactional
    public void updateActive(Long userId, Long alertId, boolean active) {
        PriceAlert alert = findOwnedAlert(alertId, userId);
        alert.changeActive(active);
    }

    /**
     * 알림 방식에 따라 목표 가격을 계산한다.
     */
    private int calculateTargetPrice(
            Game game,
            PriceAlertRequest request
    ) {
        Integer originalPrice = game.getOriginalPrice();

        if (originalPrice == null || originalPrice <= 0) {
            throw new CustomException(
                    ErrorCode.GAME_PRICE_NOT_AVAILABLE
            );
        }

        return switch (request.alertType()) {
            case RATE -> calculateRateTargetPrice(
                    originalPrice,
                    request.discountRate()
            );

            case ANY -> originalPrice - 1;
        };
    }

    /**
     * 할인율을 이용하여 목표 가격을 계산한다.
     *
     * 계산식:
     * 정가 × (100 - 할인율) / 100
     */
    private int calculateRateTargetPrice(
            int originalPrice,
            Integer discountRate
    ) {
        if (discountRate == null
                || discountRate < 1
                || discountRate > 100) {
            throw new CustomException(
                    ErrorCode.INVALID_DISCOUNT_RATE
            );
        }

        return (int) (
                (long) originalPrice
                        * (100 - discountRate)
                        / 100
        );
    }

    /**
     * 가격 알림이 요청한 사용자의 것인지 확인한다.
     */
    private PriceAlert findOwnedAlert(
            Long alertId,
            Long userId
    ) {
        PriceAlert alert = priceAlertRepository.findById(alertId)
                .orElseThrow(() ->
                        new CustomException(ErrorCode.ALERT_NOT_FOUND)
                );

        if (!alert.getUser().getId().equals(userId)) {
            throw new CustomException(
                    ErrorCode.ALERT_ACCESS_DENIED
            );
        }

        return alert;
    }
}
