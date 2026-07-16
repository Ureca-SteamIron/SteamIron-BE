package norimaets.appapiserver.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Discord 유저 정보 API(GET /api/users/@me)의 응답.
 * id: 절대 안 바뀌는 고유값(유저 식별용), username/avatar: 유저가 바꿀 수 있음(표시용)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record DiscordUserResponse(
        String id,
        String username,
        String avatar,
        String email
) {

    // avatar는 이미지 해시라서 실제 URL로 조합해줘야 한다. 아바타 미설정 유저는 null.
    //  - 아니면 "https://cdn.discordapp.com/avatars/{id}/{avatar}.png" 형태로 조합
    public String avatarUrl() {
        if (avatar == null) {
            return null;
        }

        return "https://cdn.discordapp.com/avatars/%s/%s.png"
                .formatted(id, avatar);

    }
}
