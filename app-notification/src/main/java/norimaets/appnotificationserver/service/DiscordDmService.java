package norimaets.appnotificationserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import norimaets.appnotificationserver.client.DiscordApiClient;
import norimaets.appnotificationserver.dto.DiscordDmRequest;
import norimaets.appnotificationserver.dto.DmResultStatus;
import norimaets.moduledomainmongo.document.DiscordDeliveryLog;
import norimaets.moduledomainmongo.document.DiscordDeliveryStatus;
import norimaets.moduledomainmongo.repository.DiscordDeliveryLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

/**
 * Discord DM 발송 + 중복 방지(eventKey 기준 Mongo 로그).
 *
 * 흐름:
 *   1) eventKey로 기존 로그 확인 → 이미 SENT면 ALREADY_SENT (재발송 안 함)
 *   2) 없으면(또는 이전 실패면) DM 발송 시도
 *   3) 성공 → 로그 SENT 저장 → SENT
 *      실패 → 로그 FAILED 저장 → FAILED (배치가 다음 주기에 재시도)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiscordDmService {

    private final DiscordDeliveryLogRepository logRepository;
    private final DiscordApiClient discordApiClient;

    public DmResultStatus send(DiscordDmRequest request) {
        // 1. eventKey 중복 확인 — 이미 성공 발송했으면 재발송 금지
        DiscordDeliveryLog deliveryLog = logRepository.findByEventKey(request.eventKey()).orElse(null);
        if (deliveryLog != null && deliveryLog.getStatus() == DiscordDeliveryStatus.SENT) {
            return DmResultStatus.ALREADY_SENT;
        }
        // 첫 요청이면 PENDING 로그 생성, 이전 실패건이면 그 로그 재사용 (eventKey 유니크라 새로 못 만듦)
        if (deliveryLog == null) {
            deliveryLog = logRepository.save(newPendingLog(request));
        }

        // 2. DM 발송 (① DM 채널 열기 → ② 메시지 전송)
        try {
            String channelId = discordApiClient.openDmChannel(request.discordUserId());
            String messageId = discordApiClient.sendMessage(channelId, buildMessage(request));

            deliveryLog.markSent(messageId);
            logRepository.save(deliveryLog);
            return DmResultStatus.SENT;

        } catch (RestClientResponseException e) {
            // Discord가 4xx/5xx 응답 (429 rate limit, 403 차단 등)
            String code = e.getStatusCode().value() == 429 ? "RATE_LIMITED" : "HTTP_" + e.getStatusCode().value();
            deliveryLog.markFailed(code, e.getResponseBodyAsString());
            logRepository.save(deliveryLog);
            log.error("Discord DM 발송 실패 eventKey={} code={}", request.eventKey(), code);
            return DmResultStatus.FAILED;

        } catch (Exception e) {
            // 네트워크 오류 등
            deliveryLog.markFailed("SEND_ERROR", e.getMessage());
            logRepository.save(deliveryLog);
            log.error("Discord DM 발송 오류 eventKey={}", request.eventKey(), e);
            return DmResultStatus.FAILED;
        }
    }

    private DiscordDeliveryLog newPendingLog(DiscordDmRequest r) {
        return DiscordDeliveryLog.builder()
                .eventKey(r.eventKey())
                .notificationType(r.notificationType())
                .userId(r.userId())
                .discordUserId(r.discordUserId())
                .gameId(r.gameId())
                .gameName(r.gameName())
                .targetPrice(r.targetPrice())
                .currentPrice(r.currentPrice())
                .discountPercent(r.discountPercent())
                .status(DiscordDeliveryStatus.PENDING)
                .build();
    }

    private String buildMessage(DiscordDmRequest r) {
        String current = r.currentPrice() == null ? "-" : String.format("%,d원", r.currentPrice());
        String discount = r.discountPercent() == null ? "" : " (-" + r.discountPercent() + "%)";

        if ("DISCOUNT_START".equals(r.notificationType())) {
            return """
                    🎉 찜한 게임의 할인이 시작됐어요!

                    %s
                    현재 %s%s

                    지금 확인하기 👉 https://store.steampowered.com/app/%d
                    """.formatted(r.gameName(), current, discount, r.gameId());
        }

        String target = r.targetPrice() == null ? "-" : String.format("%,d원", r.targetPrice());
        return """
                🎮 찜한 게임이 목표가에 도달했어요!

                %s
                목표가 %s → 현재 %s%s

                지금 확인하기 👉 https://store.steampowered.com/app/%d
                """.formatted(r.gameName(), target, current, discount, r.gameId());
    }
}
