package norimaets.appapiserver.service;

import lombok.RequiredArgsConstructor;
import norimaets.appapiserver.common.exception.CustomException;
import norimaets.appapiserver.common.exception.ErrorCode;
import norimaets.appapiserver.dto.request.UpdateCredentialsRequest;
import norimaets.appapiserver.dto.response.MyAccountResponse;
import norimaets.moduledomainrdb.entity.User;
import norimaets.moduledomainrdb.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 내 계정(자체 로그인 아이디/비밀번호) 관리.
 * '첫 설정'은 AuthService.setupDiscordAccount가 담당하고, 여기서는 '수정'만 다룬다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAccountService {

    private static final int MIN_PASSWORD_LENGTH = 8;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 수정 화면 진입 시: 현재 아이디 표시/미리채움용
    public MyAccountResponse getMyAccount(Long userId) {
        User user = findUser(userId);
        return new MyAccountResponse(user.getLoginId(), user.hasCredentials());
    }

    /**
     * 아이디/비밀번호 수정.
     * 1) 자체 로그인 정보가 없으면 '설정'이 먼저라 거부
     * 2) 현재 비밀번호 확인 (본인 확인)
     * 3) 아이디 변경 시 중복 체크 (본인 것은 제외)
     * 4) 새 비밀번호가 있으면 교체, 없으면 기존 유지
     */
    @Transactional
    public void updateCredentials(Long userId, UpdateCredentialsRequest request) {
        User user = findUser(userId);

        if (!user.hasCredentials()) {
            throw new CustomException(ErrorCode.CREDENTIALS_NOT_SET);
        }

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        // 아이디를 실제로 바꾸는 경우에만 중복 체크 (그대로면 본인 아이디라 통과)
        String newLoginId = request.loginId();
        if (!newLoginId.equals(user.getLoginId()) && userRepository.existsByLoginId(newLoginId)) {
            throw new CustomException(ErrorCode.LOGIN_ID_ALREADY_EXISTS);
        }

        // 새 비밀번호: 비어 있으면 기존 유지, 있으면 길이 검증 후 교체
        String encodedPassword;
        if (request.newPassword() == null || request.newPassword().isBlank()) {
            encodedPassword = user.getPassword();
        } else {
            if (request.newPassword().length() < MIN_PASSWORD_LENGTH) {
                throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
            }
            encodedPassword = passwordEncoder.encode(request.newPassword());
        }

        user.setCredentials(newLoginId, encodedPassword); // 변경 감지로 자동 UPDATE
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
