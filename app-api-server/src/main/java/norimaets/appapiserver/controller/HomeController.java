package norimaets.appapiserver.controller;

import lombok.RequiredArgsConstructor;
import norimaets.appapiserver.common.response.ApiResponse;
import norimaets.appapiserver.dto.HomeResponseDto;
import norimaets.appapiserver.service.HomeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @GetMapping("/api/home")
    public ApiResponse<HomeResponseDto> getHomeData(@AuthenticationPrincipal User user) {
        HomeResponseDto data = homeService.getHomeData(user.getId());
        return ApiResponse.success(data);
    }
}
