package com.mopl.domain.contents.writer;

import com.mopl.domain.content.entity.Content;
import com.mopl.domain.content.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TmdbWriter implements ItemWriter<Content> {

    private final ContentRepository contentRepository;

    @Override
    public void write(@NonNull Chunk<? extends Content> contents) {
        if(contents.isEmpty()) return;

        log.info("콘텐츠 DB에 저장중 {}", contents.size());

        Map<String, Content> uniqueMap = new LinkedHashMap<>();
        for (Content content : contents) {
            String key = content.getContentType() + "-" + content.getOriginId();
            uniqueMap.putIfAbsent(key, content);
        }

        List<Content> uniqueContents = new ArrayList<>(uniqueMap.values());

        log.info("Chunk dedup: {} → {}", contents.size(), uniqueContents.size());

        try {
            contentRepository.saveAll(contents.getItems());
            log.info("{}개 아이템 저장 완료", contents.size());

        } catch (Exception e) {
            log.error("DB 저장 중 오류 발생: {} | 저장 시도 아이템: {}",
                    e.getMessage(),
                    contents.getItems().stream()
                            .map(c -> c.getId() + ":" + c.getTitle())
                            .toList(),
                    e);
        }
    }
}
