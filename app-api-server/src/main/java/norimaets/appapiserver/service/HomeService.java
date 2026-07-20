package norimaets.appapiserver.service;

import lombok.RequiredArgsConstructor;
import norimaets.appapiserver.dto.request.GameFilterRequest;
import norimaets.appapiserver.dto.response.GameSimpleResponse;
import norimaets.appapiserver.dto.response.HomeResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class HomeService {
    private final GameService gameService;
    private final WishListService wishListService;

    @Transactional(readOnly = true)
    public HomeResponse getHomeData(GameFilterRequest filterCondition, Long userId) {
        // 1. 게임 서비스에서 Top 100 가져오기
//        List<GameSimpleResponse> top100 = gameService.getTop100Games();
        List<GameSimpleResponse> top100 = gameService.getFilteredTop100Games(filterCondition);

        // 2. 찜 서비스에서 내 찜 목록 가져오기 (로그인 안 했으면 빈 리스트 등 예외처리)
        List<GameSimpleResponse> myWishList = (userId != null)
                ? wishListService.getUserWishList(userId)
                : Collections.emptyList();

        // 3. 하나의 DTO로 묶어서 프론트엔드로 반환
        return new HomeResponse(top100);
    }
}