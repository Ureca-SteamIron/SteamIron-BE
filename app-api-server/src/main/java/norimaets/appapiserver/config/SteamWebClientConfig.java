package norimaets.appapiserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class SteamWebClientConfig {

    @Bean
    public WebClient steamStoreWebClient() {
        return WebClient.builder()
                .baseUrl("https://store.steampowered.com")
                .build();
    }
}