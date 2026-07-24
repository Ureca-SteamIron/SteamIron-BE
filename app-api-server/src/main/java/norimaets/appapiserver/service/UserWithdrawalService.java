package norimaets.appapiserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import norimaets.appapiserver.client.UserNotificationClient;
import org.springframework.stereotype.Service;

/**
 * 회원 탈퇴 처리(오케스트레이션).
 *
 * 정책:
 * - 하드 삭제 + 재가입 허용 (유저 로우를 지우므로 email/discordId unique 충돌이 자연히 사라진다)
 * - 작성한 댓글은 삭제하지 않고 '탈퇴한 사용자'(placeholder)로 소유권만 이전한다
 * - RDB 데이터는 한 트랜잭션에서 원자적으로 정리한다({@link UserRdbCleaner})
 * - Mongo 알림 데이터는 별도 서버(app-notification) 소유라 HTTP로 위임하며,
 *   RDB 커밋이 끝난 뒤 베스트 에포트로 삭제 요청한다(실패해도 탈퇴는 성립).
 *   (추후 Kafka 전환 시 이 위임 지점을 이벤트 발행으로 교체)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserWithdrawalService {

    private final UserRdbCleaner userRdbCleaner;
    private final UserNotificationClient userNotificationClient;

    public void withdraw(Long userId) {
        // 1) RDB 개인 데이터 정리 + 유저 삭제 (원자적 트랜잭션)
        userRdbCleaner.cleanUp(userId);

        // 2) RDB 커밋 이후: 알림 서버(Mongo)에 삭제 위임 (베스트 에포트)
        userNotificationClient.deleteAllByUserId(userId);

        log.info("회원 탈퇴 완료: userId={}", userId);
    }
}
