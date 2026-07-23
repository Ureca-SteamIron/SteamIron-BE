package norimaets.appbatchserver.dto.notification;

public record UserNotificationCreateResponse(
        UserNotificationCreateStatus status
) {

    public boolean isCompleted() {
        return status == UserNotificationCreateStatus.CREATED
                || status == UserNotificationCreateStatus.ALREADY_EXISTS;
    }
}
