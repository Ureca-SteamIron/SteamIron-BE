package norimaets.appapiserver.service;

import lombok.RequiredArgsConstructor;
import norimaets.appapiserver.client.UserNotificationClient;
import norimaets.appapiserver.client.dto.NotificationServerPageResponse;
import norimaets.appapiserver.dto.response.UnreadNotificationCountResponse;
import norimaets.appapiserver.dto.response.UserNotificationPageResponse;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserNotificationService {

    private final UserNotificationClient userNotificationClient;

    public UserNotificationPageResponse getNotifications(
            Long userId,
            int page,
            int size
    ) {
        NotificationServerPageResponse response =
                userNotificationClient.getNotifications(userId, page, size);

        return new UserNotificationPageResponse(
                response.content(),
                response.number() + 1,
                response.size(),
                response.totalElements(),
                response.totalPages()
        );
    }

    public UnreadNotificationCountResponse getUnreadCount(Long userId) {
        return userNotificationClient.getUnreadCount(userId);
    }

    public void markAsRead(String notificationId, Long userId) {
        userNotificationClient.markAsRead(notificationId, userId);
    }
}
