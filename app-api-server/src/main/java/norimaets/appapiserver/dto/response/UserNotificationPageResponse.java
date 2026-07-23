package norimaets.appapiserver.dto.response;

import java.util.List;

public record UserNotificationPageResponse(
        List<UserNotificationResponse> content,
        int currentPage,
        int size,
        long totalElements,
        int totalPages
) {
}
