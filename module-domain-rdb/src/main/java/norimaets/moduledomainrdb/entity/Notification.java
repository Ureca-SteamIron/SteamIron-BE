package norimaets.moduledomainrdb.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(length = 255, nullable = false)
    private String message;

    @Column(name = "is_read", nullable = false, columnDefinition = "boolean default false")
    private Boolean isRead;

    @Column(name = "\"type\"", length = 30, nullable = false) // 'type' 예약어 충돌 회피
    private String type;

    @CreationTimestamp
    @Column(name = "sent_at", nullable = false, updatable = false)
    private LocalDateTime sentAt;

    @Builder
    public Notification(User user, Game game, String message, String type) {
        this.user = user;
        this.game = game;
        this.message = message;
        this.type = type;
        this.isRead = false;
    }
}

