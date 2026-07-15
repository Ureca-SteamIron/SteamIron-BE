package norimaets.appapiserver.global.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
        // TODO: Authorization 헤더에서 JWT를 꺼내 검증하기
        //  1) request 에서 "Authorization" 헤더(HttpHeaders.AUTHORIZATION) 읽기
        //  2) 값이 있고 "Bearer " 로 시작하면 → 접두사(BEARER_PREFIX) 떼고 토큰만 추출
        //  3) jwtProvider.parseUserId(token) 로 검증 + userId 추출
        //       - 성공: new UsernamePasswordAuthenticationToken(userId, null, List.of()) 만들어
        //               SecurityContextHolder.getContext().setAuthentication(...) 로 인증 세팅
        //       - JwtException / IllegalArgumentException(위조·만료): 인증 세팅 없이 그냥 통과
        //  ※ 토큰이 없거나 이상해도 여기서 에러를 내지 말 것 (차단은 SecurityConfig가 담당)

        filterChain.doFilter(request, response); // 이 줄은 항상 실행돼야 함 (빠뜨리면 요청이 멈춤)
    }
}
