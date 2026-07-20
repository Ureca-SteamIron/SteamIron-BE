package norimaets.appbatchserver.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SteamSpyGameDto {

    private Long appid;
    private String name;
    private String developer;
    private String publisher;
    private Integer price;        // 문자열로 오는 경우가 있어 커스텀 역직렬화 고려
    private Integer initialprice;
    private Integer discount;
    private Integer ccu;          // 현재 동시 접속자 수
}