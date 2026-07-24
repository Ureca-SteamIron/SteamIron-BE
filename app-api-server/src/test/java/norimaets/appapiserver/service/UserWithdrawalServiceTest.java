package norimaets.appapiserver.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import norimaets.appapiserver.client.UserNotificationClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserWithdrawalServiceTest {

    private static final Long USER_ID = 7L;

    @Mock
    private UserRdbCleaner userRdbCleaner;
    @Mock
    private UserNotificationClient userNotificationClient;

    @InjectMocks
    private UserWithdrawalService userWithdrawalService;

    @Test
    @DisplayName("RDB 정리를 먼저 하고, 그 다음 알림 서버(Mongo) 삭제를 위임한다")
    void withdraw_rdbThenNotification() {
        userWithdrawalService.withdraw(USER_ID);

        InOrder inOrder = Mockito.inOrder(userRdbCleaner, userNotificationClient);
        inOrder.verify(userRdbCleaner).cleanUp(USER_ID);
        inOrder.verify(userNotificationClient).deleteAllByUserId(USER_ID);
    }

    @Test
    @DisplayName("RDB 정리가 실패하면 알림 서버 위임은 호출되지 않는다 (탈퇴 자체가 실패)")
    void withdraw_rdbFailure_skipsNotification() {
        doThrow(new RuntimeException("db down")).when(userRdbCleaner).cleanUp(USER_ID);

        assertThatThrownBy(() -> userWithdrawalService.withdraw(USER_ID))
                .isInstanceOf(RuntimeException.class);

        verify(userNotificationClient, never()).deleteAllByUserId(USER_ID);
    }
}
