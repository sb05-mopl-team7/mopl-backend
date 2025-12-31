package com.mopl.domain.contents;

import com.mopl.domain.contents.dto.SportDbDto;
import com.mopl.domain.contents.dto.TmdbDto;
import com.mopl.domain.contents.openapi.SportDbClient;
import com.mopl.domain.contents.openapi.TmdbClient;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/test")
public class TestController {

    private final TmdbClient tmdbClient;
    private final SportDbClient sportDbClient;

    @GetMapping("/tmdb")
    public Mono<List<TmdbDto>> getPopularMovies(@RequestParam(name = "page", defaultValue = "1") int page) {
        return tmdbClient.getPopularMovies(page);
    }

    @GetMapping("/sport")
    public Mono<List<SportDbDto>> getSport(@RequestParam(name = "page", defaultValue = "1") int page) {
        return sportDbClient.getSportsEventSeason();
    }
}