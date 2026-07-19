package norimaets.appapiserver.security;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.server.ResponseStatusException;

/**
 * {@link LoginUserId}가 붙은 파라미터에 로그인 유저의 userId를 넣어주는 리졸버.
 *
 * 동작 흐름:
 * 1. JwtAuthenticationFilter가 요청의 JWT를 검증하고 SecurityContext에 userId(Long)를 넣어둠
 * 2. 스프링이 컨트롤러 메서드를 호출하기 직전, @LoginUserId 파라미터를 발견하면 이 클래스를 호출
 * 3. SecurityContext에서 userId를 꺼내 파라미터로 주입
 *
 * WebConfig에 등록되어야 동작한다.
 */
@Component
public class LoginUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginUserId.class)
                && Long.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // JwtAuthenticationFilter가 인증 성공 시 principal 자리에 userId(Long)를 넣는다
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        }

        LoginUserId annotation = parameter.getParameterAnnotation(LoginUserId.class);
        if (annotation != null && annotation.required()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return null; // required = false → 비로그인 상태를 null로 표현
    }
}
