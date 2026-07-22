package norimaets.appapiserver.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import norimaets.appapiserver.common.response.ApiResponse;
import norimaets.appapiserver.dto.request.PriceAlertActiveRequest;
import norimaets.appapiserver.dto.request.PriceAlertRequest;
import norimaets.appapiserver.dto.response.PriceAlertResponse;
import norimaets.appapiserver.security.LoginUserId;
import norimaets.appapiserver.service.PriceAlertService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class PriceAlertController {

    private final PriceAlertService priceAlertService;

    // 조회: 내 알림 목록
    @GetMapping("/users/me/alerts")
    public ApiResponse<List<PriceAlertResponse>> getMyAlerts(@LoginUserId Long userId) {
        return ApiResponse.success(priceAlertService.getMyAlerts(userId));
    }

    // 생성: 특정 게임에 목표가 알림 설정
    @PostMapping("/games/{gameId}/alerts")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> create(
            @PathVariable Long gameId,
            @LoginUserId Long userId,
            @Valid @RequestBody PriceAlertRequest request
    ) {
        priceAlertService.create(userId, gameId, request);
        return ApiResponse.success();
    }

    // 수정: 목표가 변경 (본인 알림만 — 서비스에서 소유권 검증)
    @PatchMapping("/alerts/{alertId}")
    public ApiResponse<Void> updateTargetPrice(
            @PathVariable Long alertId,
            @LoginUserId Long userId,
            @Valid @RequestBody PriceAlertRequest request
    ) {
        priceAlertService.updateTargetPrice(userId, alertId, request);
        return ApiResponse.success();
    }

    // 활성화 상태 변경: 알림 켜기/끄기 (본인 알림만 — 서비스에서 소유권 검증)
    @PatchMapping("/alerts/{alertId}/active")
    public ApiResponse<Void> updateActive(
            @PathVariable Long alertId,
            @LoginUserId Long userId,
            @Valid @RequestBody PriceAlertActiveRequest request
    ) {
        priceAlertService.updateActive(userId, alertId, request.active());
        return ApiResponse.success();
    }

    // 삭제: 종 알림 OFF 시 이 게임의 할인 시작/지정 할인율 설정을 모두 제거
    @DeleteMapping("/alerts/{alertId}")
    public ApiResponse<Void> delete(
            @PathVariable Long alertId,
            @LoginUserId Long userId
    ) {
        priceAlertService.delete(userId, alertId);
        return ApiResponse.success();
    }
}
