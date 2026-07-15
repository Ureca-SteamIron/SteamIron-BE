package norimaets.appapiserver.domain.auth.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import norimaets.appapiserver.domain.auth.client.DiscordOAuthClient;
import norimaets.appapiserver.domain.auth.dto.DiscordTokenResponse;
import norimaets.appapiserver.domain.auth.dto.DiscordUserResponse;
import norimaets.appapiserver.domain.auth.dto.LoginResponse;
import norimaets.appapiserver.domain.auth.dto.ReissueResponse;
import norimaets.appapiserver.global.security.JwtProvider;
import norimaets.moduledomainrdb.auth.entity.RefreshToken;
import norimaets.moduledomainrdb.auth.repository.RefreshTokenRepository;
import norimaets.moduledomainrdb.user.entity.User;
import norimaets.moduledomainrdb.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final DiscordOAuthClient discordOAuthClient;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProvider jwtProvider;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMs;

    @Transactional
    public LoginResponse loginWithDiscord(String code) {
        // TODO: Discord 로그인 (없으면 자동 회원가입) — 흐름 ⑥
        //  1) discordOAuthClient.exchangeCode(code) → Discord access_token
        //  2) discordOAuthClient.fetchUser(accessToken) → 유저 정보 (id, username, avatarUrl, email)
        //  3) userRepository.findByDiscordId(id)
        //       - 있으면: updateProfile()로 닉네임/아바타 갱신 후 그대로 사용
        //       - 없으면: User.builder()로 새로 만들어 save() (자동 회원가입)
        //       ※ Optional 의 .map(...).orElseGet(...) 패턴을 쓰면 한 덩어리로 표현 가능
        //  4) 우리 서비스 토큰 발급: jwtProvider.createAccessToken(userId), issueRefreshToken(userId)
        //  5) LoginResponse(accessToken, refreshToken, userId, username, avatarUrl) 반환
        return null;
    }

    @Transactional
    public ReissueResponse reissue(String refreshTokenValue) {
        // TODO: 만료된 accessToken 재발급
        //  1) refreshTokenRepository.findByToken(value) → 없으면 401 (재로그인 요구)
        //  2) 만료됐으면(isExpired) → DB에서 삭제하고 401
        //  3) 새 accessToken 발급 (jwtProvider.createAccessToken(userId))
        //  4) refresh token도 새 값(UUID)으로 rotate() (토큰 로테이션, expiryDate() 사용)
        //  5) ReissueResponse(newAccessToken, newRefreshToken) 반환
        return null;
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        // TODO: DB에서 해당 refresh token 삭제 (refreshTokenRepository.deleteByToken)
        //  ※ 없는 토큰이어도 조용히 성공하는 게 정상 (에러 던지지 않음)
    }

    /**
     * refresh token 발급. 유저당 1개만 유지하는 정책이라
     * 이미 있으면 새 값으로 교체하고, 없으면 새로 저장한다.
     */
    private String issueRefreshToken(Long userId) {
        // TODO: refresh token 발급 (유저당 1개 정책)
        //  1) 새 UUID 토큰 문자열 생성
        //  2) refreshTokenRepository.findByUserId(userId)
        //       - 있으면: existing.rotate(token, expiryDate())로 교체
        //       - 없으면: new RefreshToken(token, userId, expiryDate())를 save()
        //  3) 생성한 토큰 값 반환
        return null;
    }

    private LocalDateTime expiryDate() {
        return LocalDateTime.now().plus(refreshTokenExpirationMs, ChronoUnit.MILLIS);
    }
}
