package com.mopl.domain.contents.reader;

import com.mopl.domain.contents.dto.tmdb.TvSeriesDto;
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
public class TvSeriesReader implements ItemReader<TvSeriesDto> {

    private final TmdbClient tmdbClient;
    private Iterator<TvSeriesDto> itemIterator;

    private int page = 1;
    private final int maxPage = 10;

    @Override
    public TvSeriesDto read() throws IOException {

        while (itemIterator == null || !itemIterator.hasNext()) {
            if(page > maxPage) {
                log.info("TvSeries Reader 종료 (page={})", page);
                return null;
            }

            log.info("TMDB API 호출 page={}", page);
            List<TvSeriesDto> contentList = tmdbClient.getPopularTvSeriesIdList(page).block();

            if (contentList == null || contentList.isEmpty()) {
                log.warn("TMDB page {} 응답 데이터 없음", page - 1);
                page++;
                continue;
            }

            page++;
            itemIterator = contentList.iterator();
        }

        return itemIterator.next();
    }
}
