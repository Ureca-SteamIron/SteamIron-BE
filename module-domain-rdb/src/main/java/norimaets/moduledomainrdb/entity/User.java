package norimaets.moduledomainrdb.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users") // user는 PostgreSQL 예약어라서 users로 지정
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    // Discord OAuth 식별자. 첫 로그인은 항상 Discord라 항상 존재한다.
    @Column(name = "discord_id", length = 30, nullable = false, unique = true)
    private String discordId;

    // Discord에서 받아오는 이메일. email scope 승인 시 항상 옴 → 필수·유니크.
    @Column(length = 100, nullable = false, unique = true)
    private String email;

    @Column(length = 50, nullable = false)
    private String nickname;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private Role role;

    // 로컬(아이디/비번) 로그인용. Discord 가입 후 나중에 설정 → 연동 전엔 null.
    @Column(name = "login_id", length = 30, unique = true)
    private String loginId;

    @Column(length = 255)
    private String password; // BCrypt 해시 (평문 저장 금지)

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 사용자가 discord 알람을 동의하기전까지는 false
    @Column(
            name = "discord_notification_enabled",
            nullable = false,
            columnDefinition = "boolean default false"
    )
    private boolean discordNotificationEnabled = false;

    // 탈퇴한 사용자의 댓글 소유권을 넘겨받는 placeholder 유저의 고정 식별값.
    // discordId/email은 unique+not-null이라 실제 Discord 계정과 겹치지 않는 예약값을 쓴다.
    public static final String WITHDRAWN_USER_DISCORD_ID = "__withdrawn__";
    public static final String WITHDRAWN_USER_EMAIL = "withdrawn@steamiron.internal";
    public static final String WITHDRAWN_USER_NICKNAME = "알 수 없는 사용자";

    @Builder
    public User(String discordId, String email, String nickname, String avatarUrl, Role role) {
        this.discordId = discordId;
        this.email = email;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
        this.role = role != null ? role : Role.USER;
    }

    // 탈퇴 회원 댓글 이전용 placeholder 유저 생성 (앱 최초 기동 시 1회 seeding)
    public static User createWithdrawnPlaceholder() {
        return User.builder()
                .discordId(WITHDRAWN_USER_DISCORD_ID)
                .email(WITHDRAWN_USER_EMAIL)
                .nickname(WITHDRAWN_USER_NICKNAME)
                .role(Role.USER)
                .build();
    }

    // Discord 재로그인 시 닉네임/아바타 갱신 (Discord에서 바꿨을 수 있으니)
    public void updateProfile(String nickname, String avatarUrl) {
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
    }

    // 아이디/비번 연동 (Discord 가입 후 1회 설정). password는 반드시 해시된 값을 넘길 것.
    public void setCredentials(String loginId, String encodedPassword) {
        this.loginId = loginId;
        this.password = encodedPassword;
    }

    public boolean hasCredentials() {
        return loginId != null && password != null;
    }

    public void updateDiscordNotificationEnabled(boolean enabled) {
        this.discordNotificationEnabled = enabled;
    }
}
