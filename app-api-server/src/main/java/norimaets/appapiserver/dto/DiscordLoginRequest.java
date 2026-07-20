package norimaets.appapiserver.dto;

// 프론트가 Discord 리다이렉트로 받은 code를 담아 보내는 요청.
// redirectUri: 프론트가 authorize 요청에 썼던 값. Discord는 code 교환 시 이 값이
//   authorize 때와 똑같아야 하는데, 배포 환경은 접속 IP마다 주소가 달라 고정할 수 없다.
//   그래서 프론트가 자기가 쓴 값을 그대로 실어 보내고, 백엔드는 이걸 그대로 교환에 쓴다.
public record DiscordLoginRequest(String code, String redirectUri) {
}
