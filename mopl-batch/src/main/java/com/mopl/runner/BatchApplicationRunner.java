package com.mopl.runner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.batch.autoconfigure.JobLauncherApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchApplicationRunner implements ApplicationRunner {

    private final JobLauncherApplicationRunner jobRunner;

    @Override
    public void run(ApplicationArguments args) throws Exception {

        if (args.containsOption("run.movie")) {
            log.info("movie job 실행");
            jobRunner.run("movieJob");
        }

        if (args.containsOption("run.sport")) {
            log.info("sport job 실행");
            jobRunner.run("sportJob");
        }

        if (args.containsOption("run.tvSeries")) {
            log.info("another job 실행");
            jobRunner.run("tvSeriesJob");
        }
    }
}
