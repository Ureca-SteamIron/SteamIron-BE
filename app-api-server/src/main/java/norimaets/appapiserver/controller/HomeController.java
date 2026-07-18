package norimaets.appapiserver.controller;

import lombok.RequiredArgsConstructor;
import norimaets.appapiserver.common.response.ApiResponse;
import norimaets.appapiserver.dto.request.GameFilterRequest;
import norimaets.appapiserver.dto.response.HomeResponse;
import norimaets.appapiserver.service.HomeService;
import norimaets.moduledomainrdb.entity.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @GetMapping
    public ApiResponse<HomeResponse> getHomeData(
            @ModelAttribute GameFilterRequest filterRequest
    ) {
        HomeResponse data = homeService.getHomeData(filterRequest);
        return ApiResponse.success(data);
    }
}
