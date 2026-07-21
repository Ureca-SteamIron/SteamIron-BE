package norimaets.appapiserver.controller;

import lombok.RequiredArgsConstructor;
import norimaets.appapiserver.common.response.ApiResponse;
import norimaets.appapiserver.dto.request.GameFilterRequest;
import norimaets.appapiserver.dto.response.GameSimpleResponse;
import norimaets.appapiserver.dto.response.PageResponse;
import norimaets.appapiserver.service.GameListService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/games/list")
@RequiredArgsConstructor
public class GameListController {

    private final GameListService gameListService;

    @GetMapping
    public ApiResponse<PageResponse<GameSimpleResponse>> getGames(
            GameFilterRequest filterRequest,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<GameSimpleResponse> result = gameListService.getGames(filterRequest, page - 1, size);
        return ApiResponse.success(PageResponse.from(result));
    }
}