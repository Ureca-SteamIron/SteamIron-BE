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

        return new LoginResponse(
                accessToken, refreshToken,
                user.getId(), user.getUsername(), user.getAvatarUrl());
    }

    @Transactional
    public ReissueResponse reissue(String refreshTokenValue) {
        RefreshToken saved = refreshTokenRepository.findByToken(refreshTokenValue)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "존재하지 않는 refresh token입니다. 다시 로그인해주세요."));

        if (saved.isExpired()) {
            refreshTokenRepository.delete(saved);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "만료된 refresh token입니다. 다시 로그인해주세요.");
        }

        String newAccessToken = jwtProvider.createAccessToken(saved.getUserId());
        // refresh token도 새 값으로 교체 (rotation)
        String newRefreshToken = UUID.randomUUID().toString();
        saved.rotate(newRefreshToken, expiryDate());

        return new ReissueResponse(newAccessToken, newRefreshToken);
    }

    @Transactional
    public void logout(String refreshTokenValue) {
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
                        existing -> existing.rotate(token, expiryDate()),
                        () -> refreshTokenRepository.save(
                                new RefreshToken(token, userId, expiryDate())));
        return token;
    }

    private LocalDateTime expiryDate() {
        return LocalDateTime.now().plus(refreshTokenExpirationMs, ChronoUnit.MILLIS);
    }
}
