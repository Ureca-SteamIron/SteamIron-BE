package norimaets.appapiserver.controller;

import lombok.RequiredArgsConstructor;
import norimaets.appapiserver.common.response.ApiResponse;
import norimaets.appapiserver.dto.response.GameDetailResponse;
import norimaets.appapiserver.dto.response.GameSimpleResponse;
import norimaets.appapiserver.service.GameService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    // 키워드로 전체 게임 검색 (top100 범위 제한 없음)
    @GetMapping("/search")
    public ApiResponse<List<GameSimpleResponse>> search(@RequestParam String keyword) {
        return ApiResponse.success(gameService.searchGames(keyword));
    }

    @GetMapping("{appId}")
    public ApiResponse<GameDetailResponse> getGame(@PathVariable Long appId, Long userId) {
        return ApiResponse.success(gameService.getGameDetail(appId, userId));
    }

    @PostMapping("/{appId}/refresh")
    public ApiResponse<GameDetailResponse> refreshGame(@PathVariable Long appId, Long userId) {
        return ApiResponse.success(gameService.refreshGame(appId, userId));
    }
}
