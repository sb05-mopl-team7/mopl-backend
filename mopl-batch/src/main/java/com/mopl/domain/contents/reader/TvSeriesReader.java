package com.mopl.domain.contents.reader;

import com.mopl.domain.content.enums.ContentType;
import com.mopl.domain.content.repository.ContentRepository;
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
import java.util.Set;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class TvSeriesReader implements ItemReader<TvSeriesDto> {

    private final ContentRepository contentRepository;
    private final TmdbClient tmdbClient;
    private Iterator<TvSeriesDto> itemIterator;

    private int page = 1;
    private final int maxPage = 10;

    @Override
    public TvSeriesDto read() throws IOException {
        if (itemIterator == null || !itemIterator.hasNext()) {

            if (page > maxPage) {
                log.info("TMDB Reader: 최대 페이지 제한({})에 도달하여 읽기를 종료합니다.", maxPage);
                return null;
            }

            try {
                log.info("TMDB API 호출 중... page: {}", page);
                List<TvSeriesDto> tvSeriesList = tmdbClient.getPopularTvSeriesIdList(page).block();

                if (tvSeriesList == null || tvSeriesList.isEmpty()) {
                    log.warn("페이지 {}: API로부터 응답받은 데이터가 없습니다.", page);
                    return null;
                }

                List<TvSeriesDto> newTvSeries = filterExistingTvSeries(tvSeriesList);

                // 모든 TV 시리즈가 기존 데이터면 다음 페이지로
                if (newTvSeries.isEmpty()) {
                    log.info("페이지 {}의 모든 TV 시리즈가 이미 저장되어 있습니다. 다음 페이지로 진행합니다.", page);
                    this.page++;
                    return read();
                }

                this.itemIterator = newTvSeries.iterator();
                this.page++;

            } catch (Exception e) {
                log.error("Reader에서 예외 발생 - 페이지: {}, 원인: {}", page, e.getMessage(), e);
                this.page++;
                return read();
            }
        }
        return itemIterator.next();
    }

    private List<Long> getTvSeriesIdList(List<TvSeriesDto> tvSeriesList) {
        return tvSeriesList.stream().map(TvSeriesDto::id).toList();
    }

    /**
     * 기존에 존재하는 TV 시리즈를 조회하고 제거
     * @param tvSeriesList TMDB API에서 받은 TV 시리즈 리스트
     * @return 새로운 TV 시리즈만 필터링된 리스트
     */
    private List<TvSeriesDto> filterExistingTvSeries(List<TvSeriesDto> tvSeriesList) {
        List<Long> tvSeriesIds = tvSeriesList.stream()
                .map(TvSeriesDto::id)
                .toList();

        Set<Long> existingIds = contentRepository.findExistingOriginIds(tvSeriesIds, ContentType.tvSeries);

        return tvSeriesList.stream()
                .filter(tv -> !existingIds.contains(tv.id()))
                .toList();
    }
}
