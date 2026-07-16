package norimaets.appapiserver.controller;

import lombok.RequiredArgsConstructor;
import norimaets.appapiserver.dto.DiscordLoginRequest;
import norimaets.appapiserver.dto.LoginResponse;
import norimaets.appapiserver.dto.RefreshTokenRequest;
import norimaets.appapiserver.dto.ReissueResponse;
import norimaets.appapiserver.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Discord 로그인. 프론트가 Discord 리다이렉트로 받은 code를 보내면
     * 회원가입/로그인 처리 후 우리 서비스의 토큰을 발급한다.
     */
    @PostMapping("/login/discord")
    public LoginResponse loginWithDiscord(@RequestBody DiscordLoginRequest request) {
        return authService.loginWithDiscord(request.code());
    }

    /**
     * accessToken 만료 시 refreshToken으로 새 토큰 발급.
     */
    @PostMapping("/reissue")
    public ReissueResponse reissue(@RequestBody RefreshTokenRequest request) {
        return authService.reissue(request.refreshToken());
    }

    /**
     * 로그아웃. DB의 refreshToken을 삭제해서 재발급을 막는다.
     * (accessToken은 남은 만료 시간 동안은 유효 — JWT의 특성)
     */
    @PostMapping("/logout")
    public void logout(@RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
    }
}
