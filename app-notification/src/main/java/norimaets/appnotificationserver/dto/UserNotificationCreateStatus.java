package norimaets.appnotificationserver.dto;

public enum UserNotificationCreateStatus {
    CREATED, // 첫 요청
    ALREADY_EXISTS // 같은 eventKey로 재요청
}