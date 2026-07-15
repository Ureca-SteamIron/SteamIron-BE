package norimaets.moduledomainrdb.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    // 유저당 로그인 1개만 허용하는 정책이라 unique.
    // 여러 기기 동시 로그인을 허용하게 되면 unique 제거 + 만료 토큰 배치 삭제 필요.
    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    public RefreshToken(String token, Long userId, LocalDateTime expiryDate) {
        this.token = token;
        this.userId = userId;
        this.expiryDate = expiryDate;
    }

    // 재발급 시 refresh token도 새 값으로 교체(rotation)
    public void rotate(String newToken, LocalDateTime newExpiryDate) {
        // TODO: token, expiryDate 를 전달받은 새 값으로 교체
    }

    public boolean isExpired() {
        // TODO: expiryDate 가 현재 시각(LocalDateTime.now())보다 이전이면 true (만료됨)
        return false;
    }
}
