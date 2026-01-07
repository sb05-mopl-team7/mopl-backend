package com.mopl.domain.contents.processor;

import com.mopl.domain.content.entity.Content;
import com.mopl.domain.content.enums.ContentType;
import com.mopl.domain.contents.openapi.TmdbClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class TmdbProcessor implements ItemProcessor<Long, Content> {

    private final TmdbClient tmdbClient;

    @Override
    public Content process(Long movieId) throws Exception {
        try {
            return tmdbClient.getMovieDetails(movieId)
                    .filter(movie -> movie.description() != null && !movie.description().isBlank()) // 여기서 바로 필터링
                    .map(movie -> new Content(
                            ContentType.movie,
                            movie.title(),
                            movie.description(),
                            "https://image.tmdb.org/t/p/w200" + movie.thumbnailUrl()
                    ))
                    .block();
        } catch (Exception e) {
            log.error("영화 상세 정보 조회 실패 - ID: {}, 사유: {}", movieId, e.getMessage());
            return null; // null을 리턴하면 해당 아이템은 Writer로 넘어가지 않고 필터링됨
        }
    }
}
