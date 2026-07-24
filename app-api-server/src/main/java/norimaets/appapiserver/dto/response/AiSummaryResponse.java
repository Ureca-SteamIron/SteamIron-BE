package norimaets.appapiserver.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * GET /api/games/{appId}/ai-summary 전용 응답.
 * 게임 상세(GameDetailResponse)에서 분리한 이유: Gemini 호출이 느려서(수 초) 상세 정보까지
 * 같이 기다리게 하지 않고, 프론트가 이 응답만 별도로(병렬로) 기다리게 하기 위함.
 */
@Getter
@Builder
public class AiSummaryResponse {
    private String aiExplanation;

    public static AiSummaryResponse of(String aiExplanation) {
        return AiSummaryResponse.builder()
                .aiExplanation(aiExplanation != null ? aiExplanation : "AI 요약을 불러오는 데 실패했습니다.")
                .build();
    }
}
