package com.mopl.job;

import com.mopl.domain.content.entity.Content;
import com.mopl.domain.contents.dto.sportDb.SportDbDto;
import com.mopl.domain.contents.processor.SportProcessor;
import com.mopl.domain.contents.reader.SportReader;
import com.mopl.domain.contents.writer.TmdbWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class SportBatchJob {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final SportReader sportReader;
    private final SportProcessor sportProcessor;
    private final TmdbWriter tmdbWriter;

    @Bean
    public Job sportJob() {
        return new JobBuilder("sportJob", jobRepository)
                .start(sportStep())
                .build();
    }

    @Bean
    public Step sportStep() {
        return new StepBuilder("sportStep", jobRepository)
                .<SportDbDto, Content>chunk(10)
                .reader(sportReader)
                .processor(sportProcessor)
                .writer(tmdbWriter)
                .faultTolerant()                                // 예외 허용 설정
                .skip(DataIntegrityViolationException.class)    // 중복 에러나면
                .skipLimit(100)
                .transactionManager(transactionManager)
                .build();
    }
}
