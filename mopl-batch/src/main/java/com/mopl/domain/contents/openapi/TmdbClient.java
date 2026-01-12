package com.mopl.domain.contents.openapi;

import com.mopl.domain.contents.dto.tmdb.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
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
                .bodyToMono(new ParameterizedTypeReference<TmdbResponse<TmdbDto>>() {})
                .map(response -> response.results().stream()
                        .map(TmdbDto::id)
                        .toList()
                )
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)));
    }

    public Mono<TmdbDetailDto> getMovieDetails(Long movieId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/{movie_id}")
                        .queryParam("append_to_response", "keywords")
                        .queryParam("language", "ko-KR")
                        .build(movieId))
                .retrieve()
                .bodyToMono(TmdbDetailDto.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)));
    }

    /** Open API에서 인기 있는 TV 시리즈 목록을 가져옵니다. */
    public Mono<List<TvSeriesDto>> getPopularTvSeriesIdList(int page) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/tv/popular")
                        .queryParam("page", page)
                        .queryParam("language", "ko-KR")
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<TmdbResponse<TvSeriesDto>>() {})
                .map(response -> response.results().stream()
                        .map(result-> {
                            return new TvSeriesDto(result.id(), result.title(), result.description(), result.thumbnailUrl());
                        })
                        .toList()
                )
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)));
    }

    public Mono<List<KeywordDto>> getTvSeriesKeyword(Long tvSeriesId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/tv/{tvSeriesId}")
                        .queryParam("append_to_response", "keywords")
                        .queryParam("language", "ko-KR")
                        .build(tvSeriesId))
                .retrieve()
                .bodyToMono(TvSeriesKeyword.class)
                .map(TvSeriesKeyword::genres)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)));
    }
}