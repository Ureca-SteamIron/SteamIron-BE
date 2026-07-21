package norimaets.appapiserver.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import norimaets.appapiserver.common.exception.CustomException;
import norimaets.appapiserver.common.exception.ErrorCode;
import norimaets.appapiserver.dto.response.PriceAlertResponse;
import norimaets.moduledomainrdb.entity.Game;
import norimaets.moduledomainrdb.entity.PriceAlert;
import norimaets.moduledomainrdb.entity.User;
import norimaets.moduledomainrdb.repository.GameRepository;
import norimaets.moduledomainrdb.repository.PriceAlertRepository;
import norimaets.moduledomainrdb.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 가격 알림(목표가) CRUD.
 *
 * 
 * 목표가를 "직접 받은 값"으로 저장한다.
 *
 * 목표가를 정하는 방식만 확장하면 된다. (아래 create/updateTargetPrice의 targetPrice 계산부)
 *   - 할인율 입력 
 *   - "1%라도 할인" 
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PriceAlertService {

    private final PriceAlertRepository priceAlertRepository;
    private final GameRepository gameRepository;
    private final UserRepository userRepository;

    // 내 알림 목록
    public List<PriceAlertResponse> getMyAlerts(Long userId) {
        return priceAlertRepository.findAllByUserIdWithGame(userId).stream()
                .map(PriceAlertResponse::from)
                .toList();
    }

    // 알림 생성 (같은 게임에 이미 있으면 중복 거부)
    @Transactional
    public void create(Long userId, Long gameId, Integer targetPrice) {
        if (priceAlertRepository.existsByUser_IdAndGame_Id(userId, gameId)) {
            throw new CustomException(ErrorCode.ALERT_ALREADY_EXISTS);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new CustomException(ErrorCode.GAME_NOT_FOUND));

        // TODO(현정): 방식별 목표가 계산. 정가 = game.getOriginalPrice()
        //   RATE("할인율 N%") → targetPrice = 정가 × (100 - rate) / 100
        //   ANY ("아무 할인") → targetPrice = 정가 - 1
        //   계산한 값을 아래 .targetPrice() 에 넣으면 됨. 나머지는 그대로 동작.
        priceAlertRepository.save(PriceAlert.builder()
                .user(user)
                .game(game)
                .targetPrice(targetPrice)
                .build());
    }

    // 목표가 수정 (본인 알림만)
    @Transactional
    public void updateTargetPrice(Long userId, Long alertId, Integer targetPrice) {
        PriceAlert alert = findOwnedAlert(alertId, userId);
        // TODO(현정): 여기도 방식(RATE/ANY)이면 위 create와 같은 공식으로 목표가 계산 후 넘기기.
        alert.updateTargetPrice(targetPrice); // 변경 감지(dirty checking)로 자동 UPDATE
    }

    // 알림 삭제 (본인 알림만)
    @Transactional
    public void delete(Long userId, Long alertId) {
        PriceAlert alert = findOwnedAlert(alertId, userId);
        priceAlertRepository.delete(alert);
    }

    /**
     * 소유권 확인 공통 로직 — 알림을 찾고, 그 알림이 요청한 유저의 것인지 검증한다.
     * 남의 알림을 수정/삭제하려 하면 ALERT_ACCESS_DENIED(403).
     * userId는 JWT에서 온 값(@LoginUserId)이라 신뢰할 수 있다.
     */
    private PriceAlert findOwnedAlert(Long alertId, Long userId) {
        PriceAlert alert = priceAlertRepository.findById(alertId)
                .orElseThrow(() -> new CustomException(ErrorCode.ALERT_NOT_FOUND));
        if (!alert.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.ALERT_ACCESS_DENIED);
        }
        return alert;
    }
}
