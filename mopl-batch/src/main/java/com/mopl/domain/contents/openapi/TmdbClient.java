package com.mopl.domain.contents.openapi;

import com.mopl.domain.contents.dto.tmdb.TmdbDetailDto;
import com.mopl.domain.contents.dto.tmdb.TmdbDto;
import com.mopl.domain.contents.dto.tmdb.TmdbResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class TmdbClient {

    private final WebClient webClient;

    public TmdbClient(@Qualifier("tmdbWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<List<Long>> getPopularMovieIdList(int page) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/popular")
                        .queryParam("page", page)
                        .queryParam("language", "ko-KR")
                        .build())
                .retrieve()
                .bodyToMono(TmdbResponse.class)
                .map(response -> response.results().stream()
                        .map(TmdbDto::id)
                        .toList()
                );
    }

    public Mono<TmdbDetailDto> getMovieDetails(Long movieId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/{movie_id}")
                        .queryParam("append_to_response", "keywords")
                        .queryParam("language", "ko-KR")
                        .build(movieId))
                .retrieve()
                .bodyToMono(TmdbDetailDto.class);
    }
}