package norimaets.appnotificationserver.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import norimaets.appnotificationserver.dto.DiscordDmRequest;
import norimaets.appnotificationserver.dto.DiscordDmResponse;
import norimaets.appnotificationserver.service.DiscordDmService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 배치 서버 → 알림 서버 내부 API. (외부 노출 X, 내부망 신뢰)
 * 계약: POST /internal/v1/discord/dms  →  { "status": "SENT" | "ALREADY_SENT" | "FAILED" }
 */
@RestController
@RequestMapping("/internal/v1/discord")
@RequiredArgsConstructor
public class DiscordDmController {

    private final DiscordDmService discordDmService;

    @PostMapping("/dms")
    public DiscordDmResponse sendDm(@Valid @RequestBody DiscordDmRequest request) {
        return new DiscordDmResponse(discordDmService.send(request));
    }
}
