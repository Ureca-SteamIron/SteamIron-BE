package norimaets.appapiserver.dto;

// 토큰 재발급, 로그아웃 요청에 사용
public record RefreshTokenRequest(String refreshToken) {
}
