package norimaets.appapiserver.config;

import java.util.List;
import lombok.RequiredArgsConstructor;
import norimaets.appapiserver.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity // 메서드 단위 어노테이션이 동작하려면 필수(관리자가 유저의 댓글 삭제 시 필요)
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
                        .requestMatchers(HttpMethod.GET, "/api/games/**").permitAll()          // 게임 상세도 비로그인 열람 가능, 찜 여부만 userId 있을 때 계산
                        .requestMatchers(HttpMethod.POST, "/api/games/*/refresh").permitAll() // 수동 갱신은 비로그인도 가능 (댓글 POST는 여전히 인증 필요)
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
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Tailscale IP가 팀원(디바이스)마다 달라(100.96.97.2 / 100.75.133.23 …) origin을 하나로 고정할 수 없다.
        // → 100.x 대역 8080(FE nginx)을 패턴으로 허용. IP가 늘어도 코드 수정 불필요.
        //   (setAllowedOrigins가 아니라 setAllowedOriginPatterns를 써야 와일드카드가 동작한다)
        // TODO: 실제 배포 도메인이 정해지면 그 도메인으로 교체
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:5173",
                "http://localhost:3000",
                "http://100.*:8080" // 공유 개발 서버(Tailscale)에 배포된 FE (nginx)
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
