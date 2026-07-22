package norimaets.moduledomainrdb.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "comment")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 자기 참조: 내 부모 댓글이 무엇인지 (Null이면 최상위 댓글)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    // 양방향: 내 아래에 달린 대댓글 목록
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> children = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Integer depth;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Builder
    public Comment(Comment parent, Game game, User user, Integer depth, String content) {
        this.parent = parent;
        this.game = game;
        this.user = user;
        this.depth = depth != null ? depth : 0;
        this.content = content;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    // 소프트 딜리트(Soft Delete) 용 메서드
    public void deleteComment() {
        this.deletedAt = LocalDateTime.now();
        for (Comment child : children) {
            if (child.getDeletedAt() == null) {
                child.deleteComment();  // 재귀적으로 자식까지 soft delete
            }
        }
    }
}