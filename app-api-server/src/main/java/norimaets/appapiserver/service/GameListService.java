package norimaets.appapiserver.service;

import lombok.RequiredArgsConstructor;
import norimaets.appapiserver.dto.request.GameFilterRequest;
import norimaets.appapiserver.dto.response.GameSimpleResponse;
import norimaets.appapiserver.specification.GameSpecs;
import norimaets.moduledomainrdb.entity.Game;
import norimaets.moduledomainrdb.repository.GameRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GameListService {

    private final GameRepository gameRepository;

    @Transactional(readOnly = true)
    public Page<GameSimpleResponse> getGames(GameFilterRequest request, int page, int size) {
        Sort sort = GameSpecs.resolveSort(request.getSort());
        Pageable pageable = PageRequest.of(page, size, sort);
        Specification<Game> spec = GameSpecs.baseFilter(request);

        Page<Game> gamePage = gameRepository.findAll(spec, pageable);

        return gamePage.map(GameSimpleResponse::from);
    }
}