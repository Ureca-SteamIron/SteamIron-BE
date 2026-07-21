package norimaets.appbatchserver.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AppDetailsClient {

    private final WebClient steamStoreWebClient; // baseUrl: https://store.steampowered.com

    public AppDetail fetchDetail(Long appid) {
        try {
            Map<String, AppDetailWrapper> response = steamStoreWebClient.get()
                    .uri("/api/appdetails?appids={id}&cc=kr&l=korean", appid)
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<Map<String, AppDetailWrapper>>() {})
                    .block();

            AppDetailWrapper wrapper = response != null ? response.get(String.valueOf(appid)) : null;
            if (wrapper == null || !wrapper.isSuccess() || wrapper.getData() == null) {
                return null;
            }
            return wrapper.getData();
        } catch (Exception e) {
            return null;
        }
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AppDetailWrapper {
        private boolean success;
        private AppDetail data;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AppDetail {
        private String name;

        @JsonProperty("header_image")
        private String headerImage;

        @JsonProperty("is_free")
        private Boolean isFree;

        @JsonProperty("price_overview")
        private PriceOverview priceOverview;
    }

    @Getter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PriceOverview {
        private Integer initial;

        @JsonProperty("final")
        private Integer finalPrice;

        @JsonProperty("discount_percent")
        private Integer discountPercent;
    }
}