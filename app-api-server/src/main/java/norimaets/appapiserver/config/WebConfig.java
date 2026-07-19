package norimaets.appapiserver.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/*
 *
 * 예시용 파일입니다!!
 *
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*") // 추후 실제 React 도메인으로 변경
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }
}
