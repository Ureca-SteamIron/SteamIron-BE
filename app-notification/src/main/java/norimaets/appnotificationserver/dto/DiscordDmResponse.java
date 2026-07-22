package norimaets.appnotificationserver.dto;

// 발송 결과 응답 (계약: {"status": "SENT"})
public record DiscordDmResponse(
        DmResultStatus status
) {
}
