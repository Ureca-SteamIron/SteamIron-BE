package norimaets.appapiserver.service;

import lombok.RequiredArgsConstructor;
import norimaets.appapiserver.common.exception.CustomException;
import norimaets.appapiserver.common.exception.ErrorCode;
import norimaets.moduledomainrdb.entity.User;
import norimaets.moduledomainrdb.repository.CommentRepository;
import norimaets.moduledomainrdb.repository.NotificationRepository;
import norimaets.moduledomainrdb.repository.PriceAlertRepository;
import norimaets.moduledomainrdb.repository.RefreshTokenRepository;
import norimaets.moduledomainrdb.repository.UserRepository;
import norimaets.moduledomainrdb.repository.WishListRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 탈퇴 시 RDB 개인 데이터를 원자적으로 정리한다.
 *
 * {@link UserWithdrawalService}에서 분리한 이유:
 * 같은 클래스 내부 호출은 스프링 프록시를 타지 않아 @Transactional이 무시된다(self-invocation).
 * 별도 빈으로 두어 트랜잭션 경계를 확실히 하고, Mongo(HTTP) 호출은 이 트랜잭션 바깥에서 이뤄지게 한다.
 */
@Component
@RequiredArgsConstructor
public class UserRdbCleaner {

    private final UserRepository userRepository;
    private final CommentRepository commentRepository;
    private final PriceAlertRepository priceAlertRepository;
    private final WishListRepository wishListRepository;
    private final NotificationRepository notificationRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public void cleanUp(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        // 댓글은 남기되 작성자를 placeholder로 이전 (다른 유저의 대댓글 트리 보존).
        // Comment.user_id가 nullable=false라 삭제/ null 처리 대신 예약 유저로 소유권을 넘긴다.
        Long placeholderUserId = getOrCreateWithdrawnPlaceholder().getId();
        commentRepository.reassignAuthorToPlaceholder(userId, placeholderUserId);

        // 개인 소유 데이터는 모두 삭제.
        // 위 벌크 연산들은 clearAutomatically=true라 영속성 컨텍스트를 비우므로,
        // 엔티티 참조 대신 ID로 삭제해 detached 문제를 피한다.
        priceAlertRepository.deleteAllByUserId(userId);
        wishListRepository.deleteAllByUserId(userId);
        notificationRepository.deleteAllByUserId(userId);
        refreshTokenRepository.deleteByUserId(userId);

        userRepository.deleteById(userId);
    }

    // '탈퇴한 사용자' placeholder 유저를 조회하고, 없으면 최초 1회 생성한다.
    private User getOrCreateWithdrawnPlaceholder() {
        return userRepository.findByDiscordId(User.WITHDRAWN_USER_DISCORD_ID)
                .orElseGet(() -> userRepository.save(User.createWithdrawnPlaceholder()));
    }
}
