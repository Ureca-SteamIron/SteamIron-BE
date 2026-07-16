package norimaets.appapiserver.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class HomeResponse {

    private List<GameSimpleResponse> topGames;

    private List<GameSimpleResponse> wishListGames;

}