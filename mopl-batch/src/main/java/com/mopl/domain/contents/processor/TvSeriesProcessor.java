package com.mopl.domain.contents.processor;

import com.mopl.domain.content.entity.Content;
import com.mopl.domain.content.entity.Tag;
import com.mopl.domain.content.enums.ContentType;
import com.mopl.domain.content.repository.TagRepository;
import com.mopl.domain.contents.dto.tmdb.TvSeriesDto;
import com.mopl.domain.contents.openapi.TmdbClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.mopl.domain.contents.dto.tmdb.KeywordDto.tagCache;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class TvSeriesProcessor implements ItemProcessor<TvSeriesDto, Content> {

    private final TmdbClient tmdbClient;
    private final TagRepository tagRepository;

    @BeforeStep
    public void beforeStep() {
        if (tagCache.isEmpty()) {
            List<Tag> allTags = tagRepository.findAll();
            allTags.forEach(tag -> tagCache.put(tag.getTag(), tag));
            log.info("TV 시리즈 배치를 위해 태그 {}개를 캐시에 로드했습니다.", tagCache.size());
        }
    }

    @Override
    public Content process(TvSeriesDto item) throws Exception {
        try {
            Content content = new Content(
                    ContentType.tvSeries,
                    item.title(),
                    item.description(),
                    "https://image.tmdb.org/t/p/w200" + item.thumbnailUrl()
            );

            // 2. 키워드(장르) 정보 가져오기 및 태그 매핑
            return tmdbClient.getTvSeriesKeyword(item.id())
                    .map(genreList -> {
                        genreList.forEach(genre -> {
                            Tag tag = tagCache.computeIfAbsent(genre.name(), name -> {
                                Tag newTag = new Tag(name);
                                return tagRepository.save(newTag);
                            });
                            content.addTag(tag);
                        });
                        return content;
                    })
                    .block(); // 동기 처리를 위해 block 사용

        } catch (Exception e) {
            log.error("TV 시리즈 상세 정보 처리 실패 - ID: {}, 사유: {}", item.id(), e.getMessage());
            return null; // 에러 발생 시 해당 아이템은 건너뜀
        }
    }
}