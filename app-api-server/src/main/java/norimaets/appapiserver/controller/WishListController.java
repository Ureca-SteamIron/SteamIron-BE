package norimaets.appapiserver.controller;

import lombok.RequiredArgsConstructor;
import norimaets.appapiserver.common.response.ApiResponse;
import norimaets.appapiserver.dto.response.GameSimpleResponse;
import norimaets.appapiserver.security.LoginUserId;
import norimaets.appapiserver.service.WishListService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class WishListController {

    private final WishListService wishListService;

    // 조회: 내 찜 목록
    @GetMapping("/users/me/wishlist")
    public ApiResponse<List<GameSimpleResponse>> getWishList(@LoginUserId Long userId) {
        List<GameSimpleResponse> wishList = wishListService.getUserWishList(userId);
        return ApiResponse.success(wishList);
    }

    // 추가: 찜 목록에 게임 추가
    @PostMapping("/games/{gameId}/wishlist")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> addWishList(@PathVariable Long gameId, @LoginUserId Long userId) {
        wishListService.addWishList(userId, gameId);
        return ApiResponse.success();
    }

    // 삭제: 찜 목록에서 게임 제거
    @DeleteMapping("/games/{gameId}/wishlist")
    public ApiResponse<Void> removeWishList(@PathVariable Long gameId, @LoginUserId Long userId) {
        wishListService.removeWishList(userId, gameId);
        return ApiResponse.success();
    }
}