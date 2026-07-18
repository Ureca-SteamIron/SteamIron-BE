package norimaets.appapiserver.config;

import java.util.List;
import lombok.RequiredArgsConstructor;
import norimaets.appapiserver.security.JwtAuthenticationFilter;
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
        http
                .csrf(csrf -> csrf.disable())                       // JWT 헤더 방식이라 CSRF 공격 성립 안 함 → 꺼도 안전
                .cors(cors -> {})                                   // 아래 corsConfigurationSource 빈을 사용
                .formLogin(form -> form.disable())                  // 기본 로그인 화면 불필요 (Discord 로그인만 씀)
                .httpBasic(basic -> basic.disable())                // 기본 인증 팝업 불필요
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 안 만듦, 매 요청 JWT로만 인증
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()              // 로그인 전에 호출하는 API → 토큰 검사 면제
                        .requestMatchers("/api/home").permitAll()                // 메인 top100 등 로그인 여부와 무관한 공개 데이터
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/error").permitAll()                    // 에러 응답(메시지 포함)이 보안에 막혀 빈 body 되는 것 방지
                        .anyRequest().authenticated())                            // 나머지는 기본 잠금 (인증 필요)
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, e) ->
                                response.sendError(401)))                         // 미인증 시 403 대신 401 (프론트 재발급 트리거)
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);              // 우리 JWT 필터를 기존 인증 필터 앞에 삽입
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
