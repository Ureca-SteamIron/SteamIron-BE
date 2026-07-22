package norimaets.appbatchserver.dto.notification;

public enum DiscordDmStatus {
    SENT, // 이번 요청에서 DM 발송 성공
    ALREADY_SENT, // 같은 eventKey로 이미 DM을 발송함
    FAILED // DM 발송 실패
}