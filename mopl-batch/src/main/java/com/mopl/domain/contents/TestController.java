package com.mopl.domain.contents;

import com.mopl.domain.contents.dto.sportDb.SportDbDto;
import com.mopl.domain.contents.dto.tmdb.KeywordDto;
import com.mopl.domain.contents.dto.tmdb.TmdbDetailDto;
import com.mopl.domain.contents.dto.tmdb.TvSeriesDto;
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

    @GetMapping("/tmdb/movie")
    public Mono<List<Long>> getPopularMovieIds(@RequestParam(name = "page", defaultValue = "1") int page) {
        return tmdbClient.getPopularMovieIdList(page);
    }
    @GetMapping("/tmdb/movie/{movieId}")
    public Mono<TmdbDetailDto> getMovieDetails(@PathVariable Long movieId) {
        return tmdbClient.getMovieDetails(movieId);
    }

    @GetMapping("/tmdb/tvseries")
    public Mono<List<TvSeriesDto>> getPopularTvSeriesIds(@RequestParam(name = "page", defaultValue = "1") int page) {
        return tmdbClient.getPopularTvSeriesIdList(page);
    }

    @GetMapping("/tmdb/tvseries/{tvId}")
    public Mono<List<KeywordDto>> getTvSeriesKeyword(@PathVariable Long tvId) {
        return tmdbClient.getTvSeriesKeyword(tvId);
    }

    @GetMapping("/sport")
    public Mono<List<SportDbDto>> getSport(@RequestParam(name = "page", defaultValue = "1") int page) {
        return sportDbClient.getSportsEventSeason();
    }

}