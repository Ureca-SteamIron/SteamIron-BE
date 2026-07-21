package norimaets.appbatchserver.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SteamChartsClient {

    private final WebClient steamApiWebClient; // baseUrl: https://api.steampowered.com

    public List<ChartRankDto> fetchTop100() {
        SteamChartsResponse response = steamApiWebClient.get()
                .uri("/ISteamChartsService/GetMostPlayedGames/v1/?format=json")
                .retrieve()
                .bodyToMono(SteamChartsResponse.class)
                .block();

        return response != null && response.getResponse() != null
                ? response.getResponse().getRanks()
                : List.of();
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SteamChartsResponse {
        private ChartsBody response;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChartsBody {
        private long rollupDate;
        private List<ChartRankDto> ranks;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChartRankDto {
        private Integer rank;
        private Long appid;
        private Integer lastWeekRank;
        private Integer peakInGame;
    }
}