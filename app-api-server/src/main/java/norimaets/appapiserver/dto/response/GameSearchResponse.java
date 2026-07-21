package norimaets.appapiserver.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
public class GameSearchResponse {

    private List<GameSimpleResponse> games; // 부분 일치 결과. 페이지네이션 대상.
    private List<GameSimpleResponse> similarGames; // pg_trgm 유사 검색 결과. games가 1페이지에 다 들어갈 때만 채워지는 별도 섹션(페이지네이션 미적용).
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;

    public static GameSearchResponse of(Page<GameSimpleResponse> page, List<GameSimpleResponse> similarGames) {
        return GameSearchResponse.builder()
                .games(page.getContent())
                .similarGames(similarGames)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }
}
