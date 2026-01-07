package com.mopl.domain.contents.reader;

import com.mopl.domain.contents.openapi.TmdbClient;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TmdbReader implements ItemReader<Long> {

    private final TmdbClient tmdbClient;
    private Iterator<Long> itemIterator;

    private int page = 1; // 시작 페이지

    @Override
    public Long read() throws IOException {
        if (itemIterator == null || !itemIterator.hasNext()) {
            List<Long> contentList = tmdbClient.getPopularMovieIdList(page).block();

            // 더 이상 데이터 없으면 배치 종료
            if (contentList == null || contentList.isEmpty()) {
                return null;
            }

            this.itemIterator = contentList.iterator();
            this.page++;

        }
        return itemIterator.next();
    }
}
