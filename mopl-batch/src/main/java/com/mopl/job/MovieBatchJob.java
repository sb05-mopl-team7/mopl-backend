package com.mopl.job;

import com.mopl.domain.content.entity.Content;
import com.mopl.domain.contents.processor.MovieProcessor;
import com.mopl.domain.contents.reader.MovieReader;
import com.mopl.domain.contents.writer.TmdbWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class MovieBatchJob {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final MovieReader tmdbReader;
    private final MovieProcessor tmdbProcessor;
    private final TmdbWriter tmdbWriter;

    @Bean
    public Job movieJob() {
        return new JobBuilder("movieJob", jobRepository)
                .start(movieStep())
                .build();
    }

    @Bean
    public Step movieStep() {
        return new StepBuilder("movieStep", jobRepository)
                .<Long, Content>chunk(10)
                .reader(tmdbReader)
                .processor(tmdbProcessor)
                .writer(tmdbWriter)
                .transactionManager(transactionManager)
                .build();
    }
}
