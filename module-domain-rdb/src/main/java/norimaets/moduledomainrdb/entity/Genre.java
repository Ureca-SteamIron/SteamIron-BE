package norimaets.moduledomainrdb.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "genre")
public class Genre {

    @Id
    private Long id;

    private String name;

    // 게임과의 연관관계 (양방향 매핑)
    @OneToMany(mappedBy = "genre", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GameGenre> gameGenres = new ArrayList<>();

    @Builder
    public Genre(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}