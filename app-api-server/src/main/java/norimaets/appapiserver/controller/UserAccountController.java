package norimaets.appapiserver.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import norimaets.appapiserver.common.response.ApiResponse;
import norimaets.appapiserver.dto.request.UpdateCredentialsRequest;
import norimaets.appapiserver.dto.response.MyAccountResponse;
import norimaets.appapiserver.security.LoginUserId;
import norimaets.appapiserver.service.UserAccountService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserAccountController {

    private final UserAccountService userAccountService;

    // 내 계정 정보 (수정 화면 진입 시 현재 아이디 표시용)
    @GetMapping("/account")
    public ApiResponse<MyAccountResponse> getMyAccount(@LoginUserId Long userId) {
        return ApiResponse.success(userAccountService.getMyAccount(userId));
    }

    // 아이디/비밀번호 수정 (현재 비밀번호 확인 필요 — 본인만)
    @PatchMapping("/credentials")
    public ApiResponse<Void> updateCredentials(
            @LoginUserId Long userId,
            @Valid @RequestBody UpdateCredentialsRequest request
    ) {
        userAccountService.updateCredentials(userId, request);
        return ApiResponse.success();
    }
}
