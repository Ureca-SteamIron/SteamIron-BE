package norimaets.appbatchserver;

import norimaets.appbatchserver.client.SteamSpyClient;
import norimaets.appbatchserver.dto.SteamSpyGameDto;
import norimaets.appbatchserver.job.SteamCrawlingJobConfig;
import norimaets.moduledomainrdb.entity.Game;
import norimaets.moduledomainrdb.entity.TopRanking;
import norimaets.moduledomainrdb.repository.GameRepository;
import norimaets.moduledomainrdb.repository.TopRankingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SteamCrawlingJobConfigTest {

    @Mock
    private JobRepository jobRepository;
    @Mock
    private PlatformTransactionManager transactionManager;
    @Mock
    private SteamSpyClient steamSpyClient;
    @Mock
    private GameRepository gameRepository;
    @Mock
    private TopRankingRepository topRankingRepository;

    @InjectMocks
    private SteamCrawlingJobConfig steamCrawlingJobConfig;

    private Map<String, SteamSpyGameDto> mockResponse;

    @BeforeEach
    void setUp() {
        mockResponse = new LinkedHashMap<>();

        mockResponse.put("730", SteamSpyGameDto.builder()
                .appid(730L)
                .name("Counter-Strike 2")
                .price(0)
                .initialprice(0)
                .discount(0)
                .build());

        mockResponse.put("570", SteamSpyGameDto.builder()
                .appid(570L)
                .name("Dota 2")
                .price(0)
                .initialprice(0)
                .discount(0)
                .build());

        mockResponse.put("1091500", SteamSpyGameDto.builder()
                .appid(1091500L)
                .name("Cyberpunk 2077")
                .price(5999)
                .initialprice(6999)
                .discount(15)
                .build());
    }

    @Test
    @DisplayName("신규 게임은 저장되고, TopRanking이 순위대로 생성된다")
    void executeCrawling_신규게임_저장() {
        when(steamSpyClient.fetchTop100()).thenReturn(mockResponse);
        when(gameRepository.findById(any())).thenReturn(Optional.empty());
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

        steamCrawlingJobConfig.executeCrawling();

        verify(gameRepository, times(3)).save(any(Game.class));
        verify(topRankingRepository, times(1)).deleteByCollectedDate(any());

        ArgumentCaptor<List<TopRanking>> captor = ArgumentCaptor.forClass(List.class);
        verify(topRankingRepository).saveAll(captor.capture());

        List<TopRanking> savedRankings = captor.getValue();
        assertThat(savedRankings).hasSize(3);
        assertThat(savedRankings.get(0).getRank()).isEqualTo(1);
        assertThat(savedRankings.get(1).getRank()).isEqualTo(2);
        assertThat(savedRankings.get(2).getRank()).isEqualTo(3);
    }

    @Test
    @DisplayName("이미 존재하는 게임은 새로 저장하지 않고 가격 정보만 갱신한다")
    void executeCrawling_기존게임_업데이트() {
        Game existingGame = Game.builder()
                .id(730L)
                .name("Counter-Strike 2")
                .originalPrice(0)
                .finalPrice(0)
                .discountPercent(0)
                .isFree(true)
                .build();

        when(steamSpyClient.fetchTop100()).thenReturn(mockResponse);
        when(gameRepository.findById(730L)).thenReturn(Optional.of(existingGame));
        when(gameRepository.findById(570L)).thenReturn(Optional.empty());
        when(gameRepository.findById(1091500L)).thenReturn(Optional.empty());
        when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));

        steamCrawlingJobConfig.executeCrawling();

        verify(gameRepository, never()).save(existingGame);
        verify(gameRepository, times(2)).save(any(Game.class));
    }

    @Test
    @DisplayName("SteamSpy 응답이 비어있으면 아무 작업도 하지 않는다")
    void executeCrawling_응답없음_스킵() {
        when(steamSpyClient.fetchTop100()).thenReturn(Map.of());

        steamCrawlingJobConfig.executeCrawling();

        verify(topRankingRepository, never()).deleteByCollectedDate(any());
        verify(topRankingRepository, never()).saveAll(any());
        verify(gameRepository, never()).save(any());
    }
}