package norimaets.appnotificationserver.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import norimaets.appnotificationserver.dto.UserNotificationCreateRequest;
import norimaets.appnotificationserver.dto.UserNotificationCreateStatus;
import norimaets.moduledomainmongo.document.UserNotification;
import norimaets.moduledomainmongo.document.UserNotificationType;
import norimaets.moduledomainmongo.repository.UserNotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserNotificationServiceTest {

    @Mock
    private UserNotificationRepository userNotificationRepository;

    @InjectMocks
    private UserNotificationService userNotificationService;

    @Test
    void createsNotificationWhenEventKeyDoesNotExist() {
        UserNotificationCreateRequest request = createRequest();

        when(userNotificationRepository.findByEventKey(request.eventKey()))
                .thenReturn(Optional.empty());

        UserNotificationCreateStatus status =
                userNotificationService.create(request);

        assertThat(status).isEqualTo(UserNotificationCreateStatus.CREATED);
        verify(userNotificationRepository).save(any(UserNotification.class));
    }

    @Test
    void doesNotCreateDuplicateNotification() {
        UserNotificationCreateRequest request = createRequest();

        UserNotification existingNotification = UserNotification.builder()
                .eventKey(request.eventKey())
                .notificationType(UserNotificationType.TARGET_PRICE)
                .userId(1L)
                .gameId(10L)
                .gameName("테스트 게임")
                .currentPrice(10000)
                .build();

        when(userNotificationRepository.findByEventKey(request.eventKey()))
                .thenReturn(Optional.of(existingNotification));

        UserNotificationCreateStatus status =
                userNotificationService.create(request);

        assertThat(status)
                .isEqualTo(UserNotificationCreateStatus.ALREADY_EXISTS);

        verify(userNotificationRepository, never())
                .save(any(UserNotification.class));
    }

    @Test
    void marksNotificationAsRead() {
        UserNotification notification = UserNotification.builder()
                .eventKey("price-alert:1:target_price:10000")
                .notificationType(UserNotificationType.TARGET_PRICE)
                .userId(1L)
                .gameId(10L)
                .gameName("테스트 게임")
                .currentPrice(10000)
                .build();

        when(userNotificationRepository.findByIdAndUserId("notification-id", 1L))
                .thenReturn(Optional.of(notification));

        boolean updated =
                userNotificationService.markAsRead("notification-id", 1L);

        assertThat(updated).isTrue();
        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getReadAt()).isNotNull();

        verify(userNotificationRepository).save(notification);
    }

    @Test
    void doesNotMarkNotificationAsReadWhenItDoesNotExist() {
        when(userNotificationRepository.findByIdAndUserId("notification-id", 1L))
                .thenReturn(Optional.empty());

        boolean updated =
                userNotificationService.markAsRead("notification-id", 1L);

        assertThat(updated).isFalse();

        verify(userNotificationRepository, never())
                .save(any(UserNotification.class));
    }

    @Test
    void returnsUnreadNotificationCount() {
        when(userNotificationRepository.countByUserIdAndReadFalse(1L))
                .thenReturn(3L);

        long unreadCount =
                userNotificationService.getUnreadCount(1L);

        assertThat(unreadCount).isEqualTo(3L);
    }

    private UserNotificationCreateRequest createRequest() {
        return new UserNotificationCreateRequest(
                "price-alert:1:target_price:10000",
                UserNotificationType.TARGET_PRICE,
                1L,
                1L,
                10L,
                "테스트 게임",
                15000,
                10000,
                33
        );
    }
}
