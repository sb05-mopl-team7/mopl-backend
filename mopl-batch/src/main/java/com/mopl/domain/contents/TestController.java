package com.mopl.domain.contents;

import com.mopl.domain.contents.dto.SportDbDto;
import com.mopl.domain.contents.dto.tmdb.TmdbDetailDto;
import com.mopl.domain.contents.openapi.SportDbClient;
import com.mopl.domain.contents.openapi.TmdbClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

//@Profile("local")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/test")
public class TestController {

    private final TmdbClient tmdbClient;
    private final SportDbClient sportDbClient;

    @GetMapping("/tmdb")
    public Mono<List<Long>> getPopularMovieIds(@RequestParam(name = "page", defaultValue = "1") int page) {
        return tmdbClient.getPopularMovieIdList(page);
    }
    @GetMapping("/tmdb/{movieId}")
    public Mono<TmdbDetailDto> getMovieDetails(@PathVariable Long movieId) {
        return tmdbClient.getMovieDetails(movieId);
    }

    @GetMapping("/sport")
    public Mono<List<SportDbDto>> getSport(@RequestParam(name = "page", defaultValue = "1") int page) {
        return sportDbClient.getSportsEventSeason();
    }
}