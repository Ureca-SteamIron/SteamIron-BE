package norimaets.moduledomainrdb.repository;

import norimaets.moduledomainrdb.entity.WishList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WishListRepository extends JpaRepository<WishList, Long> {

    // 특정 유저가 특정 게임을 이미 찜했는지 확인 (추가 시 중복 방지용)
    boolean existsByUser_IdAndGame_Id(Long userId, Long gameId);

    // 특정 게임을 찜한 전체 유저 수 (게임 상세에 "찜 N개"로 노출)
    long countByGame_Id(Long gameId);

    // 특정 유저의 특정 게임 찜 항목 조회 (삭제/단건 조회용)
    Optional<WishList> findByUser_IdAndGame_Id(Long userId, Long gameId);

    // 특정 유저의 전체 찜 목록 조회 (N+1 방지를 위해 Game, GameGenre, Genre까지 fetch join)
    @Query("SELECT DISTINCT w FROM WishList w " +
            "JOIN FETCH w.game g " +
            "LEFT JOIN FETCH g.gameGenres gg " +
            "LEFT JOIN FETCH gg.genre " +
            "WHERE w.user.id = :userId")
    List<WishList> findAllByUserIdWithGame(@Param("userId") Long userId);

    // 삭제 시 벌크 삭제 (deleteByUser_IdAndGame_GameId 조합)
    void deleteByUser_IdAndGame_Id(Long userId, Long gameId);

    // 회원 탈퇴 시 해당 유저의 찜 목록 전체 삭제
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM WishList w WHERE w.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}