package norimaets.appapiserver.controller;

import lombok.RequiredArgsConstructor;
import norimaets.appapiserver.common.response.ApiResponse;
import norimaets.appapiserver.dto.request.GameFilterRequest;
import norimaets.appapiserver.dto.response.GameDetailResponse;
import norimaets.appapiserver.dto.response.GameSearchResponse;
import norimaets.appapiserver.service.GameService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    // 키워드로 전체 게임 검색 (top100 범위 제한 없음). size 기본값은 스팀 검색 결과와 동일하게 25.
    // 정렬/필터는 top100과 동일한 GameFilterRequest를 그대로 재사용 (games에만 적용, similarGames는 미적용)
    @GetMapping("/search")
    public ApiResponse<GameSearchResponse> search(
            @RequestParam String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @ModelAttribute GameFilterRequest filterRequest
    ) {
        return ApiResponse.success(gameService.searchGames(keyword, page, size, filterRequest));
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
