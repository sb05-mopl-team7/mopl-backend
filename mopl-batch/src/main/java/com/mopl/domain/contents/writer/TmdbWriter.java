package com.mopl.domain.contents.writer;

import com.mopl.domain.content.entity.Content;
import com.mopl.domain.content.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TmdbWriter implements ItemWriter<Content> {

    private final ContentRepository contentRepository;

    @Override
    public void write(@NonNull Chunk<? extends Content> contents) {
        if(contents.isEmpty()) return;

        log.info("콘텐츠 DB에 저장중 {}", contents.size());
        try {
            contentRepository.saveAll(contents.getItems());
        } catch (Exception e) {
            log.error("DB 저장 중 오류 발생: {}", e.getMessage());
            throw e;
        }
    }
}
