package norimaets.appnotificationserver.dto;

/**
 * 발송 결과 (계약).
 *  - SENT        : 이번 요청으로 DM 발송 성공
 *  - ALREADY_SENT: 같은 eventKey로 이미 성공 발송한 기록이 있음 (재발송 안 함)
 *  - FAILED      : 발송 실패
 * 배치는 SENT/ALREADY_SENT일 때만 lastNotifiedPrice를 갱신한다.
 */
public enum DmResultStatus {
    SENT,
    ALREADY_SENT,
    FAILED
}
