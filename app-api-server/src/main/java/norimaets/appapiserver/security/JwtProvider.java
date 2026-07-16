package norimaets.appapiserver.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {

    private final SecretKey key;
    private final long accessTokenExpirationMs;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpirationMs
    ) {
        // HS256 서명용 키. secret은 32바이트(글자) 이상이어야 한다.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    public String createAccessToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))                              // 토큰 주인 = 우리 DB의 userId
                .issuedAt(now)                                                // 발급 시각
                .expiration(new Date(now.getTime() + accessTokenExpirationMs)) // 만료 시각
                .signWith(key)                                                // 비밀키로 서명
                .compact();                                                   // 최종 문자열로 조립
    }

    /**
     * 토큰의 서명과 만료를 검증하고 userId를 꺼낸다.
     * 위조/만료 토큰이면 JwtException이 던져진다.
     */
    public Long parseUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)            // 서명 검증 (만료 검사도 자동으로 됨)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.parseLong(claims.getSubject());
    }
}
