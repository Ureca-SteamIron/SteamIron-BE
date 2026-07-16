package norimaets.appapiserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/*
 * 인증(JWT/OAuth2)이 아직 구현되지 않은 임시 설정입니다.
 * spring-boot-starter-security 의존성만 있고 커스텀 설정이 없으면
 * Spring Boot가 기본값으로 모든 요청에 로그인을 요구해 API가 전부 401로 막힙니다.
 * 실제 인증 로직이 붙으면 이 설정은 교체되어야 합니다.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
}
