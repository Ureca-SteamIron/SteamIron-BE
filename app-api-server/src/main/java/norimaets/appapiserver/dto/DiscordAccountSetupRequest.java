package norimaets.appapiserver.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DiscordAccountSetupRequest(
        @NotBlank(message = "Discord 계정 설정 인증 정보가 없습니다.")
        String accountSetupToken,

        @NotBlank(message = "로그인 아이디를 입력해주세요.")
        @Size(min = 4, max = 30, message = "로그인 아이디는 4자 이상 30자 이하여야 합니다.")
        String loginId,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다.")
        String password
) {
}
