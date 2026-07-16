package norimaets.appapiserver.service;

import lombok.RequiredArgsConstructor;
import norimaets.appapiserver.dto.response.GameSimpleResponse;
import norimaets.moduledomainrdb.entity.Game;
import norimaets.moduledomainrdb.repository.GameRepository;
import norimaets.moduledomainrdb.repository.TopRankingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;
    private final TopRankingRepository topRankingRepository;

    @Transactional(readOnly = true)
    public List<GameSimpleResponse> getTop100Games() {

        List<Game> top100Games = topRankingRepository.findTodayTop100Games();

        return top100Games.stream()
                .map(GameSimpleResponse::from)
                .collect(Collectors.toList());
    }
}
