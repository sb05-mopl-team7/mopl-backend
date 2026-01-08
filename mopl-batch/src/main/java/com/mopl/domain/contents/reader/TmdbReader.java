package com.mopl.domain.contents.reader;

import com.mopl.domain.contents.openapi.TmdbClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class TmdbReader implements ItemReader<Long> {

    private final TmdbClient tmdbClient;
    private Iterator<Long> itemIterator;

    private int page = 1;
    private final int maxPage = 20;

    @Override
    public Long read() throws IOException {
        if (itemIterator == null || !itemIterator.hasNext()) {

            if (page > maxPage) {
                log.info("TMDB Reader: 최대 페이지 제한({})에 도달하여 읽기를 종료합니다.", maxPage);
                return null;
            }

            log.debug("TMDB API 호출 중... page: {}", page);
            List<Long> contentList = tmdbClient.getPopularMovieIdList(page).block();

            // 더 이상 데이터 없으면 배치 종료
            if (contentList == null || contentList.isEmpty()) return null;

            this.itemIterator = contentList.iterator();
            this.page++;

        }
        return itemIterator.next();
    }
}
