package com.mopl.domain.contents.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

//@Profile("local")
@RequiredArgsConstructor
@RestController
@RequestMapping("/batch")
public class BatchController {

    private final JobLauncher jobLauncher;
    private final Job movieJob;
    private final Job tvSeriesJob;
    private final Job sportJob;

    @GetMapping("/tmdb/movie")
    public String runMovieBatch() {
        return executeJob(movieJob, "Movie");
    }

    @GetMapping("/tmdb/tvseries")
    public String runTvSeriesBatch() {
        return executeJob(tvSeriesJob, "TV Series");
    }

    @GetMapping("/sport")
    public String runSportBatch() {
        return executeJob(sportJob, "Sport");
    }

    private String executeJob(Job job, String jobName) {
        CompletableFuture.runAsync(() -> {
            try {
                JobParameters jobParameters = new JobParametersBuilder()
                        .addString("datetime", LocalDateTime.now().toString())
                        .toJobParameters();

                jobLauncher.run(job, jobParameters);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return "SUCCESS: TMDB " + jobName + " Batch execution triggered.";
    }
}
