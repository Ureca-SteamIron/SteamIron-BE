package norimaets.appapiserver.security;

import io.swagger.v3.oas.annotations.Parameter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 파라미터에 붙이면 JWT에서 꺼낸 로그인 유저의 userId(Long)가 주입된다.
 * 실제 주입 로직은 {@link LoginUserIdArgumentResolver}가 수행한다.
 *
 * <pre>
 * // 로그인 필수 API — 비로그인이면 자동으로 401
 * public WishlistResponse myWishlist(@LoginUserId Long userId) { ... }
 *
 * // 로그인 선택 API — 비로그인이면 userId에 null이 들어옴
 * public GameDetailResponse detail(@LoginUserId(required = false) Long userId) { ... }
 * </pre>
 *
 * 팀 규칙: 유저가 누구인지 필요한 API는 전부 이걸로 받는다.
 * userId를 쿼리 파라미터/바디로 받으면 안 된다 (다른 유저 행세가 가능해짐).
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Parameter(hidden = true)
public @interface LoginUserId {

    /** false면 비로그인 요청도 허용하고 null을 주입한다. 기본값은 로그인 필수(true). */
    boolean required() default true;
}
