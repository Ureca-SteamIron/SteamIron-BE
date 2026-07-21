package norimaets.appbatchserver.collector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import norimaets.appbatchserver.dto.DiscountSnapshot;
import norimaets.appbatchserver.dto.SteamDiscountItem;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

/**
 * 스팀 "지금 할인 중인 전체 목록"을 수집한다.
 * 응답은 {"total_count": n, "results_html": "<a>...</a>..."} 형태라
 * ① Jackson으로 total_count / results_html 을 꺼내고
 * ② results_html(HTML 조각)을 Jsoup으로 파싱해 게임별 정보를 뽑는다.
 */
@Slf4j
@Component
public class SteamDiscountCollector {

    // 배치 서버엔 웹 스타터가 없어 ObjectMapper 빈이 자동 등록되지 않는다. 직접 생성해 쓴다.
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 한 번에 100개씩, start를 100씩 늘려가며 total_count 도달까지 반복
    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGES = 300;       // 안전장치(무한루프 방지). 100*300=3만개까지 커버
    private static final long PAGE_DELAY_MS = 1500;  // 페이지 사이 간격. 너무 빠르면 스팀이 429로 막는다
    private static final int MAX_RETRIES = 4;        // 429(rate limit) 시 재시도 횟수
    private static final long RETRY_BASE_MS = 5000;  // 재시도 대기(횟수에 비례해 5s,10s,15s...)

    private static final String BASE_URL =
            "https://store.steampowered.com/search/results/"
                    + "?query&specials=1&infinite=1&cc=kr&l=koreana&count=" + PAGE_SIZE + "&start=";

    public DiscountSnapshot fetchAllDiscounts() {
        List<SteamDiscountItem> result = new ArrayList<>();
        int start = 0;
        int total = Integer.MAX_VALUE;
        int page = 0;

        while (start < total && page < MAX_PAGES) {
            try {
                String json = fetchPage(start);
                JsonNode root = objectMapper.readTree(json);
                total = root.path("total_count").asInt(0);
                String html = root.path("results_html").asText("");

                List<SteamDiscountItem> parsed = parseRows(html);
                if (parsed.isEmpty()) break; // 더 이상 항목 없으면 종료
                result.addAll(parsed);

                start += PAGE_SIZE;
                page++;
                Thread.sleep(PAGE_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("할인 목록 수집이 중단되었습니다.", e);
            } catch (Exception e) {
                throw new RuntimeException("할인 목록 수집 실패 (start=" + start + ")", e);
            }
        }

        // total_count 지점까지 도달했으면 완전 수집. MAX_PAGES/빈 페이지로 중간에 멈췄으면 불완전.
        boolean complete = start >= total;
        log.info("할인 목록 수집: 총 {}건 (total_count={}, 완전수집={})", result.size(), total, complete);
        return new DiscountSnapshot(result, total, complete);
    }

    // 429(rate limit)가 나면 점점 길게 쉬며 재시도한다. 그래도 안 되면 예외를 던져 배치 실패로.
    private String fetchPage(int start) throws Exception {
        int attempt = 0;
        while (true) {
            try {
                return Jsoup.connect(BASE_URL + start)
                        .ignoreContentType(true)                 // JSON 응답이라 HTML 아님 → 무시
                        .userAgent("Mozilla/5.0 (SteamIron batch)")
                        .timeout(10_000)
                        .execute()
                        .body();
            } catch (org.jsoup.HttpStatusException e) {
                if (e.getStatusCode() == 429 && attempt < MAX_RETRIES) {
                    attempt++;
                    long backoff = RETRY_BASE_MS * attempt; // 5s, 10s, 15s, 20s
                    log.warn("스팀 429(rate limit) start={} → {}ms 대기 후 재시도 {}/{}",
                            start, backoff, attempt, MAX_RETRIES);
                    Thread.sleep(backoff);
                    continue;
                }
                throw e;
            }
        }
    }

    /**
     * results_html 안의 각 게임 row를 파싱. (실제 스팀 응답 구조에 맞춰 검증 완료)
     * 예시 구조:
     *   <a class="search_result_row" data-ds-appid="1623730">
     *     <span class="title">Palworld</span>
     *     <div class="discount_block" data-discount="30" data-price-final="2240000">
     *       <div class="discount_pct">-30%</div>
     *       <div class="discount_original_price">₩ 32,000</div>
     *       <div class="discount_final_price">₩ 22,400</div>
     *     </div>
     *   </a>
     * 가격/할인율은 텍스트 대신 data-* 속성을 우선 사용(통화기호·콤마 파싱 회피, 더 안정적).
     */
    private List<SteamDiscountItem> parseRows(String html) {
        List<SteamDiscountItem> items = new ArrayList<>();
        Document doc = Jsoup.parse(html);

        for (Element row : doc.select("a.search_result_row")) {
            Long appId = parseAppId(row.attr("data-ds-appid"));
            if (appId == null) continue; // 번들 등 appid 파싱 불가 → skip

            Element titleEl = row.selectFirst("span.title");
            String name = titleEl != null ? titleEl.text() : "";

            Element discountBlock = row.selectFirst("div.discount_block");
            if (discountBlock == null) continue; // 할인 정보 없는 행 → skip

            // data-discount="30" → 30
            int discountPercent = parseNumber(discountBlock.attr("data-discount"));

            // data-price-final 은 통화 최소단위(×100). ₩22,400 → "2240000" → 22400
            String rawFinal = discountBlock.attr("data-price-final");
            Integer finalPrice = rawFinal.isBlank() ? null : parseNumber(rawFinal) / 100;

            // 정가는 별도 data 속성이 없어 텍스트에서 추출 ("₩ 32,000" → 32000)
            Element originalEl = row.selectFirst("div.discount_original_price");
            Integer originalPrice = originalEl != null ? nullIfZero(parseNumber(originalEl.text())) : null;

            items.add(new SteamDiscountItem(appId, name, originalPrice, finalPrice, discountPercent));
        }
        return items;
    }

    // data-ds-appid 는 보통 "730". 번들이면 "12,34" 처럼 여러 개 → 단일 게임만 다루므로 skip
    private Long parseAppId(String raw) {
        if (raw == null || raw.isBlank() || raw.contains(",")) return null;
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // "₩ 10,000" / "-50%" 등에서 숫자만 추출. 값 없으면 0.
    private int parseNumber(String text) {
        if (text == null) return 0;
        String digits = text.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? 0 : Integer.parseInt(digits);
    }

    private Integer nullIfZero(int v) {
        return v == 0 ? null : v;
    }
}
