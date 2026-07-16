package norimaets.appapiserver.service;

import lombok.RequiredArgsConstructor;
import norimaets.appapiserver.dto.GameSimpleResponse;
import norimaets.appapiserver.dto.HomeResponseDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HomeService {
    // Repository 대신 각 도메인의 전담 Service를 주입받습니다.
    private final GameService gameService;
    private final WishListService wishListService;

    public HomeResponseDto getHomeData(Long userId) {
        // 1. 게임 서비스에서 Top 100 가져오기
        List<GameSimpleResponse> top100 = gameService.getTop100Games();

        // 2. 찜 서비스에서 내 찜 목록 가져오기 (로그인 안 했으면 빈 리스트 등 예외처리)
        List<GameSimpleResponse> myWishList = wishListService.getUserWishList(userId);

        // 3. 하나의 DTO로 묶어서 프론트엔드로 반환
        return new HomeResponseDto(top100, myWishList);
    }
}