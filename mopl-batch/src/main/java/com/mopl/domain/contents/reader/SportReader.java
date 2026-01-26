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
    private boolean initialized = false;

    @Override
    public SportDbDto read() {

        if(!initialized) {
            initialized = true;

            log.info("Sport DB API 호출 시작");

            List<SportDbDto> sportList;
            try {
                sportList = sportDbClient.getSportsEventSeason().block();
            } catch (Exception e) {
                log.error("Sport DB API 호출 실패", e);
                throw e;
            }

            if (sportList == null || sportList.isEmpty()) {
                log.warn("Sport DB API 응답 데이터 없음");
                return null; // 정상 종료
            }

            // API 응답 내 중복 제거
            Set<Long> seen = new HashSet<>();
            List<SportDbDto> deduped = sportList.stream()
                    .filter(dto -> seen.add(dto.id()))
                    .toList();

            log.info("Sport API {}건 → dedup {}건", sportList.size(), deduped.size());
            itemIterator = deduped.iterator();
        }

        return (itemIterator != null && itemIterator.hasNext())
                ? itemIterator.next()
                : null;
    }
}
