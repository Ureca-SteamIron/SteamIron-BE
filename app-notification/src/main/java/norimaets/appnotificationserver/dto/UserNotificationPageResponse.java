package norimaets.appnotificationserver.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record UserNotificationPageResponse(
        List<UserNotificationResponse> content,
        int number,
        int size,
        long totalElements,
        int totalPages
) {

    public static UserNotificationPageResponse from(
            Page<UserNotificationResponse> page
    ) {
        return new UserNotificationPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
