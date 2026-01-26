package com.mopl.runner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchApplicationRunner implements ApplicationRunner {

    private final JobLauncher jobLauncher;
    private final ApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (args.containsOption("run.movie")) {
            runJob("movieJob");
        }
        if (args.containsOption("run.sport")) {
            runJob("sportJob");
        }
        if (args.containsOption("run.tvSeries")) {
            runJob("tvSeriesJob");
        }
    }

    private void runJob(String jobName) {
        try {
            log.info(">>>>>> {} 시작 시도", jobName);

            // ApplicationContext에서 빈 이름으로 직접 Job을 찾아옵니다.
            Job job = applicationContext.getBean(jobName, Job.class);

            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(job, params);
            log.info(">>>>>> {} 완료", jobName);
        } catch (Exception e) {
            log.error(">>>>>> {} 실행 중 에러 발생: {}", jobName, e.getMessage());
        }
    }
}
