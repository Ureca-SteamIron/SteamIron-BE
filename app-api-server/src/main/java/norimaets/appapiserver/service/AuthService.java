package norimaets.appapiserver.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import norimaets.appapiserver.client.DiscordOAuthClient;
import norimaets.appapiserver.dto.DiscordTokenResponse;
import norimaets.appapiserver.dto.DiscordUserResponse;
import norimaets.appapiserver.dto.LoginResponse;
import norimaets.appapiserver.dto.ReissueResponse;
import norimaets.appapiserver.security.JwtProvider;
import norimaets.moduledomainrdb.entity.RefreshToken;
import norimaets.moduledomainrdb.repository.RefreshTokenRepository;
import norimaets.moduledomainrdb.entity.User;
import norimaets.moduledomainrdb.repository.UserRepository;
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
        // 1. code를 Discord access_token으로 교환
        DiscordTokenResponse discordToken = discordOAuthClient.exchangeCode(code);

        // 2. Discord 유저 정보 조회 (Discord 토큰은 여기까지만 쓰고 버림)
        DiscordUserResponse discordUser = discordOAuthClient.fetchUser(discordToken.accessToken());

        // 3. discordId로 조회 → 있으면 로그인(프로필 갱신), 없으면 자동 회원가입
        User user = userRepository.findByDiscordId(discordUser.id())
                .map(existing -> {
                    existing.updateProfile(discordUser.username(), discordUser.avatarUrl());
                    return existing;
                })
                .orElseGet(() -> userRepository.save(User.builder()
                        .discordId(discordUser.id())
                        .username(discordUser.username())
                        .avatarUrl(discordUser.avatarUrl())
                        .email(discordUser.email())
                        .build()));

        // 4. 우리 서비스 토큰 발급
        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = issueRefreshToken(user.getId());

        return new LoginResponse(accessToken, refreshToken,
                user.getId(), user.getUsername(), user.getAvatarUrl());
    }

    @Transactional
    public ReissueResponse reissue(String refreshTokenValue) {
        RefreshToken saved = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "존재하지 않는 refresh token입니다. 다시 로그인해주세요."));

        if (saved.isExpired()) {
            refreshTokenRepository.delete(saved);                    // 만료된 토큰은 삭제하고
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "만료된 refresh token입니다. 다시 로그인해주세요.");   // 재로그인 요구
        }

        String newAccessToken = jwtProvider.createAccessToken(saved.getUserId());
        String newRefreshToken = UUID.randomUUID().toString();
        saved.rotate(newRefreshToken, expiryDate());                // refresh token도 교체 (rotation)
        // rotate 후 save()를 따로 안 부르는 이유: @Transactional 안 변경 감지(dirty checking)로 자동 UPDATE

        return new ReissueResponse(newAccessToken, newRefreshToken);
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        // 없는 토큰이어도 조용히 성공하는 게 정상 (에러 던지지 않음)
        refreshTokenRepository.deleteByToken(refreshTokenValue);
    }

    /**
     * refresh token 발급. 유저당 1개만 유지하는 정책이라
     * 이미 있으면 새 값으로 교체하고, 없으면 새로 저장한다.
     */
    private String issueRefreshToken(Long userId) {
        String token = UUID.randomUUID().toString();
        refreshTokenRepository.findByUserId(userId)
                .ifPresentOrElse(
                        existing -> existing.rotate(token, expiryDate()),          // 있으면 새 값으로 교체 (rotation)
                        () -> refreshTokenRepository.save(new RefreshToken(token, userId, expiryDate())) // 없으면 새로 저장
                );
        return token;
    }

    private LocalDateTime expiryDate() {
        return LocalDateTime.now().plus(refreshTokenExpirationMs, ChronoUnit.MILLIS);
    }
}
