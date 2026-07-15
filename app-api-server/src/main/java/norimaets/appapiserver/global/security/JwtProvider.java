package norimaets.appapiserver.global.security;

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
        // TODO: userId를 담은 JWT accessToken 만들기
        //  - Jwts.builder() 사용
        //  - subject = String.valueOf(userId)  (토큰 주인 = 우리 DB의 userId)
        //  - issuedAt = 지금, expiration = 지금 + accessTokenExpirationMs
        //  - signWith(key) 로 서명 후 compact() 로 문자열 반환
        return null;
    }

    /**
     * 토큰의 서명과 만료를 검증하고 userId를 꺼낸다.
     * 위조/만료 토큰이면 JwtException이 던져진다.
     */
    public Long parseUserId(String token) {
        // TODO: 토큰의 서명·만료를 검증하고 userId 꺼내기
        //  - Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload() 로 Claims 추출
        //  - 위조/만료면 JwtException이 던져짐 (여기서 잡지 말 것 — 호출한 필터에서 처리)
        //  - claims.getSubject() 를 Long으로 변환해 반환
        return null;
    }
}
