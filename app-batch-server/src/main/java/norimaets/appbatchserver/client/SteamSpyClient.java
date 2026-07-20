package norimaets.appbatchserver.client;

import norimaets.appbatchserver.dto.SteamSpyGameDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import lombok.RequiredArgsConstructor;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SteamSpyClient {

    @Qualifier("steamSpyWebClient")
    private final WebClient webClient;

    public Map<String, SteamSpyGameDto> fetchTop100() {
        return webClient.get()
                .uri("/api.php?request=top100in2weeks")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, SteamSpyGameDto>>() {})
                .block();
    }
}