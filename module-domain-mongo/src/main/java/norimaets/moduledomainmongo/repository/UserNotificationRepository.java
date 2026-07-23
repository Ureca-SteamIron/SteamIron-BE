package norimaets.moduledomainmongo.repository;

import java.util.Optional;
import norimaets.moduledomainmongo.document.UserNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserNotificationRepository
        extends MongoRepository<UserNotification, String> {

    Optional<UserNotification> findByEventKey(String eventKey);

    // 사용자 알림 최신순 조회
    Page<UserNotification> findAllByUserIdOrderByCreatedAtDesc(
            Long userId,
            Pageable pageable
    );

    // 본인 알림인지 조회
    Optional<UserNotification> findByIdAndUserId(
            String id,
            Long userId
    );

    long countByUserIdAndReadFalse(Long userId);
}