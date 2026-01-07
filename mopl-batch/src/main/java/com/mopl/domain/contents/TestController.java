package com.mopl.domain.contents;

import com.mopl.domain.contents.dto.SportDbDto;
import com.mopl.domain.contents.dto.tmdb.TmdbDetailDto;
import com.mopl.domain.contents.openapi.SportDbClient;
import com.mopl.domain.contents.openapi.TmdbClient;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

//@Profile("local")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/test")
public class TestController {

    private final TmdbClient tmdbClient;
    private final SportDbClient sportDbClient;

    private final JobLauncher jobLauncher;
    private final Job TmdbJob;

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

    @GetMapping("/batch/tmdb")
    public String runTmdbBatch() {
        // 별도의 스레드에서 배치를 실행하여 WebFlux 루프 스레드가 차단되지 않도록 합니다.
        CompletableFuture.runAsync(() -> {
            try {
                JobParameters jobParameters = new JobParametersBuilder()
                        .addString("datetime", LocalDateTime.now().toString())
                        .toJobParameters();

                jobLauncher.run(TmdbJob, jobParameters);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return "SUCCESS: TMDB Batch execution triggered in a background thread.";
    }
}