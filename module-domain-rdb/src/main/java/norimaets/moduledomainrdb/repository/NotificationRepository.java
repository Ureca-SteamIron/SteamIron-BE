package norimaets.moduledomainrdb.repository;

import norimaets.moduledomainrdb.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // 회원 탈퇴 시 해당 유저의 RDB 알림 전체 삭제
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM Notification n WHERE n.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
