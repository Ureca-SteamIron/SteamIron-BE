package norimaets.appapiserver.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 자체 로그인 아이디/비밀번호 수정 요청. (이미 설정된 유저가 변경할 때)
 *  - currentPassword: 본인 확인용. 반드시 검증한다.
 *  - newPassword: 비어 있으면 비밀번호는 그대로 두고 아이디만 바꾼다. (길이 검증은 값이 있을 때만 서비스에서)
 */
public record UpdateCredentialsRequest(
        @NotBlank(message = "아이디를 입력해주세요.")
        @Size(min = 4, max = 30, message = "아이디는 4~30자여야 합니다.")
        String loginId,

        @NotBlank(message = "현재 비밀번호를 입력해주세요.")
        String currentPassword,

        String newPassword
) {
}
