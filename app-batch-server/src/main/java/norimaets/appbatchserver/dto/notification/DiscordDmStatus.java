package norimaets.appbatchserver.dto.notification;

public enum DiscordDmStatus {
    SENT, // 이번 요청에서 DM 발송 성공 (동기 응답이 있을 때만 해당)
    ALREADY_SENT, // 같은 eventKey로 이미 DM을 발송함
    QUEUED, // 카프카에 발행 완료. 실제 발송 성공/실패는 알림 서버 쪽에서 비동기로 처리됨
    FAILED // 카프카 발행 자체가 실패함 (직렬화 오류, 브로커 연결 실패 등)
}