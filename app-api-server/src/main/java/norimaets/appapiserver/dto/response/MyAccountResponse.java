package norimaets.appapiserver.dto.response;

/**
 * 내 계정 정보. 수정 화면에서 현재 아이디를 미리 채우고, 설정 여부를 판단하는 데 쓴다.
 *  - loginId: 자체 로그인 아이디 (미설정이면 null)
 *  - hasCredentials: 아이디/비밀번호가 설정돼 있는지 (false면 '수정'이 아니라 '설정'을 해야 함)
 */
public record MyAccountResponse(
        String loginId,
        boolean hasCredentials
) {
}
