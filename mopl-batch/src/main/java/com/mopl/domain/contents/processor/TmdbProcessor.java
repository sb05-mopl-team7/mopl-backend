package com.mopl.domain.contents.processor;

import com.mopl.domain.contents.dto.ContentDto;
import com.mopl.domain.contents.dto.tmdb.TmdbDetailDto;
import com.mopl.domain.contents.dto.tmdb.TmdbDto;
import com.mopl.domain.contents.openapi.TmdbClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class TmdbProcessor implements ItemProcessor<TmdbDto, ContentDto> {

    private final TmdbClient tmdbClient;

    @Override
    public ContentDto process(TmdbDto tmdbDto) throws Exception {

        Mono<TmdbDetailDto> movie = tmdbClient.getMovieDetails(tmdbDto.id());
        return null;
    }
}
