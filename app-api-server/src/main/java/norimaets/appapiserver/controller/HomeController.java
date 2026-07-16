package norimaets.appapiserver.controller;

import lombok.RequiredArgsConstructor;
import norimaets.appapiserver.common.response.ApiResponse;
import norimaets.appapiserver.dto.response.HomeResponse;
import norimaets.appapiserver.service.HomeService;
import norimaets.moduledomainrdb.entity.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @GetMapping("/api/home")
    public ApiResponse<HomeResponse> getHomeData(@AuthenticationPrincipal User user) {
        HomeResponse data = homeService.getHomeData(user.getId());
        return ApiResponse.success(data);
    }
}
