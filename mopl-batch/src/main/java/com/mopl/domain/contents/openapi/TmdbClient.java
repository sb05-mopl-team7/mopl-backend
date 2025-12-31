package com.mopl.domain.contents.openapi;

import com.mopl.domain.contents.dto.TmdbDto;
import com.mopl.domain.contents.dto.TmdbResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
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

    public Mono<List<TmdbDto>> getPopularMovies(int page) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/popular")
                        .queryParam("page", page)
                        .queryParam("language", "ko-KR")
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<TmdbResponse<TmdbDto>>() {})
                .map(response -> response.results().stream()
                    .filter(dto -> dto.description() != null && !dto.description().isBlank())
                    .toList()
                );
    }
}