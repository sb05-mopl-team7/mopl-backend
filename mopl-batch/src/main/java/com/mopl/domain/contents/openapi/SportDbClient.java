package com.mopl.domain.contents.openapi;

import com.mopl.domain.contents.dto.sportDb.SportDbDto;
import com.mopl.domain.contents.dto.sportDb.SportDbResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

@Component
public class SportDbClient {

    private final WebClient webClient;

    public SportDbClient(@Qualifier("sportDbWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<List<SportDbDto>> getSportsEventSeason() {

        int currentYear = LocalDate.now().getYear();
        String seasonParam = (currentYear - 1) + "-" + (currentYear);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/json/123/eventsseason.php")
                        .queryParam("id", "4328")
                        .queryParam("s", seasonParam)
                        .build())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<SportDbResponse<SportDbDto>>() {
                })
                .map(SportDbResponse::events);

    }
}
