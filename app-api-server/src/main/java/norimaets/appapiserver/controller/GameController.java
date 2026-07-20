package norimaets.appapiserver.controller;

import lombok.RequiredArgsConstructor;
import norimaets.appapiserver.common.response.ApiResponse;
import norimaets.appapiserver.dto.response.GameDetailResponse;
import norimaets.appapiserver.service.GameService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @GetMapping("{appId}")
    public ApiResponse<GameDetailResponse> getGame(@PathVariable Long appId, Long userId) {
        return ApiResponse.success(gameService.getGameDetail(appId, userId));
    }
}
