package norimaets.moduledomainrdb.repository;

import norimaets.moduledomainrdb.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // user_id로 작성한 댓글 조회 (게임 정보도 함께 필요하므로 game fetch join)
    @EntityGraph(attributePaths = {"game"})
    Page<Comment> findByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // 최상위 댓글 페이징 조회 (children까지 fetch join)
    @EntityGraph(attributePaths = {"children", "user", "children.user"})
    Page<Comment> findByGame_IdAndParentIsNull(Long gameId, Pageable pageable);

    // 특정 댓글 + 작성자 정보 함께 조회 (권한 체크 등에 사용)
    @EntityGraph(attributePaths = {"user"})
    Optional<Comment> findById(Long id);

    // 부모 댓글이 삭제되지 않았는지 확인하며 대댓글 작성 시 사용
    @Query("select c from Comment c where c.id = :id and c.deletedAt is null")
    Optional<Comment> findActiveById(@Param("id") Long id);

    // 게임 상세페이지에 노출할 전체 댓글 수 (삭제된 것 제외)
    long countByGame_IdAndDeletedAtIsNull(Long gameId);

    // 특정 게임의 모든 댓글을 한 번에 가져와서 애플리케이션 레벨에서 트리 구성할 때 사용
    @Query("select c from Comment c where c.game.id = :gameId order by c.createdAt asc")
    List<Comment> findAllByGameIdOrderByCreatedAtAsc(@Param("gameId") Long gameId);

    // 관리자 강제 삭제용 - soft delete 여부 무관하게 조회
    Optional<Comment> findByIdAndGame_Id(Long id, Long gameId);
}