package norimaets.appapiserver.controller;

import lombok.RequiredArgsConstructor;
import norimaets.appapiserver.common.response.ApiResponse;
import norimaets.appapiserver.dto.response.GameSimpleResponse;
import norimaets.appapiserver.service.WishListService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class WishListController {

    // TODO: 로그인 기능 구현 후 제거하고 @AuthenticationPrincipal 등으로 교체
    private static final Long TEST_USER_ID = 1L;

    private final WishListService wishListService;

    // 조회: 내 찜 목록
    @GetMapping("/users/me/wishlist")
    public ApiResponse<List<GameSimpleResponse>> getWishList() {
        List<GameSimpleResponse> wishList = wishListService.getUserWishList(TEST_USER_ID);
        return ApiResponse.success(wishList);
    }

    // 추가: 찜 목록에 게임 추가
    @PostMapping("/games/{gameId}/wishlist")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> addWishList(@PathVariable Long gameId) {
        wishListService.addWishList(TEST_USER_ID, gameId);
        return ApiResponse.success();
    }

    // 삭제: 찜 목록에서 게임 제거
    @DeleteMapping("/games/{gameId}/wishlist")
    public ApiResponse<Void> removeWishList(@PathVariable Long gameId) {
        wishListService.removeWishList(TEST_USER_ID, gameId);
        return ApiResponse.success();
    }
}