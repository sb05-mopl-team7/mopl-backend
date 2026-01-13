package com.mopl.job;

import com.mopl.domain.content.entity.Content;
import com.mopl.domain.contents.dto.tmdb.TvSeriesDto;
import com.mopl.domain.contents.processor.TvSeriesProcessor;
import com.mopl.domain.contents.reader.TvSeriesReader;
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
public class TvSeriesBatchJob {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final TvSeriesReader tvSeriesReader;
    private final TvSeriesProcessor tvSeriesProcessor;
    private final TmdbWriter tmdbWriter;

    @Bean
    public Job tvSeriesJob() {
        return new JobBuilder("tvSeriesJob", jobRepository)
                .start(tvSeriesStep())
                .build();
    }

    @Bean
    public Step tvSeriesStep() {
        return new StepBuilder("tvSeriesStep", jobRepository)
                .<TvSeriesDto, Content>chunk(10)
                .reader(tvSeriesReader)
                .processor(tvSeriesProcessor)
                .writer(tmdbWriter)
                .transactionManager(transactionManager)
                .build();
    }
}
