package norimaets.appapiserver.dto;

// 프론트가 Discord 리다이렉트로 받은 code를 담아 보내는 요청
public record DiscordLoginRequest(String code) {
}
