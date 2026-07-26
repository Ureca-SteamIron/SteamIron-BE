package norimaets.appapiserver.controller;

import lombok.RequiredArgsConstructor;
import norimaets.appapiserver.common.response.ApiResponse;
import norimaets.appapiserver.dto.request.TestPriceAlertRequest;
import norimaets.appapiserver.service.TestService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 카프카 알림 파이프라인 테스트 전용 컨트롤러.
 * 배치 스케줄(가격 수집 배치 실행 시점)을 기다리지 않고 바로 이벤트를 발행해서 확인할 수 있다.
 */
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final TestService testService;

    @PostMapping("/kafka/price-alert")
    public ApiResponse<Boolean> publishPriceAlert(@RequestBody TestPriceAlertRequest request) {
        boolean published = testService.publishTestPriceAlert(request);
        return ApiResponse.success(published);
    }
}
