package com.mopl.domain.contents.reader;

import com.mopl.domain.contents.dto.sportDb.SportDbDto;
import com.mopl.domain.contents.openapi.SportDbClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class SportReader implements ItemReader<SportDbDto> {

    private final SportDbClient sportDbClient;
    private Iterator<SportDbDto> itemIterator;


    @Override
    public SportDbDto read() {
        if (itemIterator == null) {
            try {
                log.info("Sport DB API 호출 시작");
                List<SportDbDto> sportList = sportDbClient.getSportsEventSeason().block();

                if (sportList == null || sportList.isEmpty()) {
                    log.warn("API로부터 응답받은 데이터가 없습니다.");
                    return null;
                }

                Set<Long> seen = new HashSet<>();

                List<SportDbDto> deduped = sportList.stream()
                        .filter(dto -> seen.add(dto.id()))
                        .toList();

                log.info("Sport API {}건 → dedup {}건", sportList.size(), deduped.size());
                itemIterator = deduped.iterator();

            } catch (Exception e) {
                log.error("Reader에서 예외 발생 - 원인: {}", e.getMessage(), e);
                return null;
            }
        }
        return itemIterator.hasNext() ? itemIterator.next() : null;
    }
}
