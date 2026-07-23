package norimaets.appapiserver.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import norimaets.appapiserver.dto.response.UserNotificationResponse;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NotificationServerPageResponse(
        List<UserNotificationResponse> content,
        int number,
        int size,
        long totalElements,
        int totalPages
) {
}
