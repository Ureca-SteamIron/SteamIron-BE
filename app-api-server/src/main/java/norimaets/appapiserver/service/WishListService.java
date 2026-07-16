package norimaets.appapiserver.service;

import lombok.RequiredArgsConstructor;
import norimaets.appapiserver.common.exception.CustomException;
import norimaets.appapiserver.common.exception.ErrorCode;
import norimaets.appapiserver.dto.response.GameSimpleResponse;
import norimaets.moduledomainrdb.entity.Game;
import norimaets.moduledomainrdb.entity.User;
import norimaets.moduledomainrdb.entity.WishList;
import norimaets.moduledomainrdb.repository.GameRepository;
import norimaets.moduledomainrdb.repository.UserRepository;
import norimaets.moduledomainrdb.repository.WishListRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishListService {

    private final WishListRepository wishListRepository;
    private final UserRepository userRepository;
    private final GameRepository gameRepository;

    // 전체 찜 목록 조회
    public List<GameSimpleResponse> getUserWishList(Long userId) {
        return wishListRepository.findAllByUserIdWithGame(userId)
                .stream()
                .map(wishList -> GameSimpleResponse.from(wishList.getGame()))
                .toList();
    }

    // 찜 목록에 게임 추가
    @Transactional
    public void addWishList(Long userId, Long gameId) {
        if (wishListRepository.existsByUser_IdAndGame_Id(userId, gameId)) {
            throw new CustomException(ErrorCode.WISHLIST_NOT_FOUND);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new CustomException(ErrorCode.GAME_NOT_FOUND));

        WishList wishList = WishList.builder()
                .user(user)
                .game(game)
                .build();

        wishListRepository.save(wishList);
    }

    // 찜 목록에서 삭제
    @Transactional
    public void removeWishList(Long userId, Long gameId) {
        WishList wishList = wishListRepository.findByUser_IdAndGame_Id(userId, gameId)
                .orElseThrow(() -> new CustomException(ErrorCode.WISHLIST_NOT_FOUND));

        wishListRepository.delete(wishList);
    }

}
