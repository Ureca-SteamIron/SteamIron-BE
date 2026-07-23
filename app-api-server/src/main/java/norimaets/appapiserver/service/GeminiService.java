package norimaets.appapiserver.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GeminiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Value("${gemini.api.key}")
    private String apiKey;

    public String generateGameSummary(String gameName, Integer originalPrice, Integer discountPercent) {
        String prompt = String.format(
                "'%s'라는 스팀 게임에 대해 설명해줘. 원가는 %d원이고 현재 %d%% 할인 중이야. " +
                        "이 게임의 핵심 재미 요소와 현재 할인 가격에 대한 평가를 포함해서 딱 3줄로 재미있게 요약해줘.",
                gameName, originalPrice, discountPercent
        );

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        String finalUrl = apiUrl + "?key=" + apiKey;

        try {
            URI uri = URI.create(finalUrl);
            // JsonNode 대신 String으로 받아서 직접 파싱 → 메시지 컨버터 의존성 제거
            ResponseEntity<String> response = restTemplate.postForEntity(uri, requestEntity, String.class);

            String body = response.getBody();
            if (body != null) {
                JsonNode root = objectMapper.readTree(body);
                JsonNode textNode = root.at("/candidates/0/content/parts/0/text");
                if (!textNode.isMissingNode()) {
                    return textNode.asText();
                }
            }
        } catch (Exception e) {
            System.err.println("Gemini API 호출 중 에러 발생: " + e.getMessage());
        }

        return "AI 요약을 불러오는 데 실패했습니다.";
    }
}