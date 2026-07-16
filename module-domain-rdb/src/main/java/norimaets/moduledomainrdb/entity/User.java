package norimaets.moduledomainrdb.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users") // user는 PostgreSQL 예약어라서 users로 지정
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Discord가 부여한 고유 ID(snowflake). 유저 식별은 항상 이 값으로 한다.
    @Column(nullable = false, unique = true)
    private String discordId;

    @Column(nullable = false)
    private String username;

    private String avatarUrl;

    private String email;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public User(String discordId, String username, String avatarUrl, String email) {
        this.discordId = discordId;
        this.username = username;
        this.avatarUrl = avatarUrl;
        this.email = email;
        this.createdAt = LocalDateTime.now();
    }

    // Discord에서 닉네임/아바타를 바꿨을 수 있으니 로그인할 때마다 갱신
    public void updateProfile(String username, String avatarUrl) {
        this.username = username;
        this.avatarUrl = avatarUrl;
    }
}
