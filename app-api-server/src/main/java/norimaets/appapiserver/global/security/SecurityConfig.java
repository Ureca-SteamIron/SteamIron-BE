package norimaets.appapiserver.global.security;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // TODO: 보안 규칙 설정 (http 빌더 체이닝 후 http.build() 반환)
        //  - csrf 비활성화 (JWT 방식이라 불필요), cors 활성화
        //  - formLogin / httpBasic 비활성화 (Discord 로그인만 씀)
        //  - 세션 STATELESS (매 요청 JWT로만 인증, 서버가 로그인 상태 저장 안 함)
        //  - 경로별 인가 (authorizeHttpRequests):
        //      /api/auth/** , /actuator/** , /swagger-ui/** , /v3/api-docs/**  → permitAll
        //      그 외 anyRequest → authenticated  (기본은 잠그고 예외만 연다)
        //  - 미인증 시 403 대신 401 응답 (exceptionHandling → authenticationEntryPoint)
        //  - jwtAuthenticationFilter 를 UsernamePasswordAuthenticationFilter 앞에 추가
        //    (addFilterBefore)
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // TODO: 프론트 도메인이 정해지면 여기에 추가
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:3000"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
