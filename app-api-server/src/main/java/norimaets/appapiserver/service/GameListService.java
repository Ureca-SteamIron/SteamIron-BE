package norimaets.appapiserver.service;

import lombok.RequiredArgsConstructor;
import norimaets.appapiserver.common.sort.GameNameSort;
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
        String sortType = request.getSort();
        Specification<Game> spec = GameSpecs.baseFilter(request);

        Pageable pageable;
        if (GameNameSort.isNameSort(sortType)) {
            // 이름순은 "한글→영어→기타" 그룹 우선순위가 필요해 Sort 객체로 표현할 수 없다.
            // Specification의 query.orderBy에 CASE 식을 심고 Pageable에는 정렬을 싣지 않는다.
            spec = spec.and(GameNameSort.orderSpec("name_desc".equals(sortType)));
            pageable = PageRequest.of(page, size);
        } else {
            Sort sort = GameSpecs.resolveSort(sortType);
            pageable = PageRequest.of(page, size, sort);
        }

        Page<Game> gamePage = gameRepository.findAll(spec, pageable);

        return gamePage.map(GameSimpleResponse::from);
    }
}