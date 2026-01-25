package com.mopl.domain.contents.reader;

import com.mopl.domain.content.enums.ContentType;
import com.mopl.domain.content.repository.ContentRepository;
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
public class MovieReader implements ItemReader<Long> {

    private final ContentRepository contentRepository;
    private final TmdbClient tmdbClient;
    private Iterator<Long> itemIterator;

    private int page = 1;
    private final int maxPage = 3;

    @Override
    public Long read() throws IOException {
        if (itemIterator == null || !itemIterator.hasNext()) {

            if (page > maxPage) {
                log.info("TMDB Reader: 최대 페이지 제한({})에 도달하여 읽기를 종료합니다.", maxPage);
                return null;
            }

            try {
                log.info("TMDB API 호출 중... page: {}", page);
                List<Long> contentList = tmdbClient.getPopularMovieIdList(page).block();

                if (contentList == null || contentList.isEmpty()) {
                    log.warn("페이지 {}: API로부터 응답받은 데이터가 없습니다.", page);
                    return null;
                }

                Set<Long> existingMovieIds =  contentRepository.findExistingOriginIds(contentList, ContentType.movie);

                List<Long> newMovies = contentList.stream()
                        .filter(id -> !existingMovieIds.contains(id))
                        .toList();

                // 모든 영화가 기존 데이터면 다음 페이지로
                if (newMovies.isEmpty()) {
                    log.info("페이지 {}의 모든 영화가 이미 저장되어 있습니다. 다음 페이지로 진행합니다.", page);
                    this.page++;
                    return read();  // 재귀 호출로 다음 페이지 처리
                }

                this.itemIterator = newMovies.iterator();
                this.page++;

            } catch (Exception e) {
                log.error("Reader에서 예외 발생 - 페이지: {}, 원인: {}", page, e.getMessage(), e);
                this.page++;
                return read();
            }
        }
        return itemIterator.next();
    }
}
