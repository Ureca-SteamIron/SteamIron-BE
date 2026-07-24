package norimaets.appapiserver.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import norimaets.appapiserver.common.exception.CustomException;
import norimaets.appapiserver.common.exception.ErrorCode;
import norimaets.moduledomainrdb.entity.User;
import norimaets.moduledomainrdb.repository.CommentRepository;
import norimaets.moduledomainrdb.repository.NotificationRepository;
import norimaets.moduledomainrdb.repository.PriceAlertRepository;
import norimaets.moduledomainrdb.repository.RefreshTokenRepository;
import norimaets.moduledomainrdb.repository.UserRepository;
import norimaets.moduledomainrdb.repository.WishListRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserRdbCleanerTest {

    private static final Long USER_ID = 10L;
    private static final Long PLACEHOLDER_ID = 999L;

    @Mock
    private UserRepository userRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private PriceAlertRepository priceAlertRepository;
    @Mock
    private WishListRepository wishListRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private UserRdbCleaner cleaner() {
        return new UserRdbCleaner(
                userRepository,
                commentRepository,
                priceAlertRepository,
                wishListRepository,
                notificationRepository,
                refreshTokenRepository
        );
    }

    // 이미 존재하는 placeholder 유저를 반환하도록 스텁
    private void stubExistingPlaceholder() {
        User placeholder = Mockito.mock(User.class);
        when(placeholder.getId()).thenReturn(PLACEHOLDER_ID);
        when(userRepository.findByDiscordId(User.WITHDRAWN_USER_DISCORD_ID))
                .thenReturn(Optional.of(placeholder));
    }

    @Test
    @DisplayName("존재하지 않는 유저를 탈퇴시키면 USER_NOT_FOUND 예외가 나고 아무 데이터도 건드리지 않는다")
    void withdraw_userNotFound() {
        when(userRepository.existsById(USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> cleaner().cleanUp(USER_ID))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND);

        verify(commentRepository, never()).reassignAuthorToPlaceholder(any(), any());
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("탈퇴 시 개인 데이터는 삭제하고, 댓글은 placeholder 유저로 이전한 뒤 유저를 삭제한다")
    void withdraw_deletesPersonalDataAndReassignsComments() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        stubExistingPlaceholder();

        cleaner().cleanUp(USER_ID);

        // 댓글은 삭제가 아니라 placeholder로 소유권 이전
        verify(commentRepository).reassignAuthorToPlaceholder(USER_ID, PLACEHOLDER_ID);

        // 개인 소유 데이터는 모두 삭제
        verify(priceAlertRepository).deleteAllByUserId(USER_ID);
        verify(wishListRepository).deleteAllByUserId(USER_ID);
        verify(notificationRepository).deleteAllByUserId(USER_ID);
        verify(refreshTokenRepository).deleteByUserId(USER_ID);

        // 마지막에 유저 삭제
        verify(userRepository).deleteById(USER_ID);
    }

    @Test
    @DisplayName("댓글 이전은 반드시 유저 삭제보다 먼저 실행된다 (FK 위반 방지)")
    void withdraw_reassignBeforeUserDelete() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        stubExistingPlaceholder();

        cleaner().cleanUp(USER_ID);

        InOrder inOrder = Mockito.inOrder(commentRepository, userRepository);
        inOrder.verify(commentRepository).reassignAuthorToPlaceholder(USER_ID, PLACEHOLDER_ID);
        inOrder.verify(userRepository).deleteById(USER_ID);
    }

    @Test
    @DisplayName("placeholder 유저가 없으면 최초 1회 생성해서 사용한다")
    void withdraw_createsPlaceholderWhenMissing() {
        when(userRepository.existsById(USER_ID)).thenReturn(true);
        when(userRepository.findByDiscordId(User.WITHDRAWN_USER_DISCORD_ID))
                .thenReturn(Optional.empty());

        User saved = Mockito.mock(User.class);
        when(saved.getId()).thenReturn(PLACEHOLDER_ID);
        when(userRepository.save(any(User.class))).thenReturn(saved);

        cleaner().cleanUp(USER_ID);

        verify(userRepository).save(any(User.class));
        verify(commentRepository).reassignAuthorToPlaceholder(USER_ID, PLACEHOLDER_ID);
    }

    @Test
    @DisplayName("생성되는 placeholder 유저는 예약 discordId/email/nickname을 가진다")
    void placeholderFactory_usesReservedValues() {
        User placeholder = User.createWithdrawnPlaceholder();

        assertThat(placeholder.getDiscordId()).isEqualTo(User.WITHDRAWN_USER_DISCORD_ID);
        assertThat(placeholder.getEmail()).isEqualTo(User.WITHDRAWN_USER_EMAIL);
        assertThat(placeholder.getNickname()).isEqualTo(User.WITHDRAWN_USER_NICKNAME);
    }
}
