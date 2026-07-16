package norimaets.appapiserver.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import norimaets.moduledomainrdb.entity.Role;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 모든 요청에서 Authorization 헤더의 JWT를 검사하는 필터.
 * 유효한 토큰이면 SecurityContext에 인증 정보(userId)를 넣어주고,
 * 없거나 유효하지 않으면 인증 없이 통과시킨다 → 보호된 API라면 뒤에서 401 처리됨.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()); // "Bearer " 잘라내기
            try {
                Long userId = jwtProvider.parseUserId(token);        // 검증 + userId 추출
                Role role = jwtProvider.parseRole(token);            // 권한 추출
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId, null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))); // ROLE_ 접두사 필수
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException e) {
                // 위조/만료 토큰 → 인증 세팅 없이 통과 (미인증 상태로 처리됨)
            }
        }

        filterChain.doFilter(request, response); // 이 줄은 항상 실행돼야 함 (빠뜨리면 요청이 멈춤)
    }
}
