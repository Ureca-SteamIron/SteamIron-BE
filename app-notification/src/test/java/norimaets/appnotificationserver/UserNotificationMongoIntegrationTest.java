package norimaets.appnotificationserver;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import norimaets.appnotificationserver.dto.UserNotificationCreateRequest;
import norimaets.appnotificationserver.dto.UserNotificationCreateStatus;
import norimaets.appnotificationserver.dto.UserNotificationResponse;
import norimaets.appnotificationserver.service.UserNotificationService;
import norimaets.moduledomainmongo.document.UserNotificationType;
import norimaets.moduledomainmongo.repository.UserNotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "SPRING_MONGODB_URI", matches = ".+")
class UserNotificationMongoIntegrationTest {

    @Autowired
    private UserNotificationService userNotificationService;

    @Autowired
    private UserNotificationRepository userNotificationRepository;

    @Test
    void createsListsAndMarksNotificationAsRead() {
        String eventKey = "integration-test-" + UUID.randomUUID();
        long userId = -System.currentTimeMillis();

        UserNotificationCreateRequest request =
                new UserNotificationCreateRequest(
                        eventKey,
                        UserNotificationType.TARGET_PRICE,
                        999_999L,
                        userId,
                        730L,
                        "통합 테스트 게임",
                        20_000,
                        19_000,
                        50
                );

        try {
            UserNotificationCreateStatus status =
                    userNotificationService.create(request);

            assertThat(status)
                    .isEqualTo(UserNotificationCreateStatus.CREATED);
            assertThat(userNotificationService.getUnreadCount(userId))
                    .isEqualTo(1L);

            Page<UserNotificationResponse> page =
                    userNotificationService.getNotifications(
                            userId,
                            PageRequest.of(0, 20)
                    );

            assertThat(page.getContent()).hasSize(1);
            UserNotificationResponse notification = page.getContent().get(0);
            assertThat(notification.gameName()).isEqualTo("통합 테스트 게임");
            assertThat(notification.read()).isFalse();

            boolean updated = userNotificationService.markAsRead(
                    notification.id(),
                    userId
            );

            assertThat(updated).isTrue();
            assertThat(userNotificationService.getUnreadCount(userId))
                    .isZero();
        } finally {
            userNotificationRepository.findByEventKey(eventKey)
                    .ifPresent(userNotificationRepository::delete);
        }
    }
}
