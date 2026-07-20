package norimaets.appbatchserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean("steamSpyWebClient")
    public WebClient steamSpyWebClient() {
        return WebClient.builder()
                .baseUrl("https://steamspy.com")
                .build();
    }
}