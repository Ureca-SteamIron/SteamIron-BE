package norimaets.appbatchserver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean("steamApiWebClient")
    public WebClient steamApiWebClient() {
        return WebClient.builder()
                .baseUrl("https://api.steampowered.com")
                .build();
    }

    @Bean("steamStoreWebClient")
    public WebClient steamStoreWebClient() {
        return WebClient.builder()
                .baseUrl("https://store.steampowered.com")
                .build();
    }

    @Bean("notificationWebClient")
    public WebClient notificationWebClient(
            @Value("${notification.server.base-url:http://localhost:8082}")
            String baseUrl
    ) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}