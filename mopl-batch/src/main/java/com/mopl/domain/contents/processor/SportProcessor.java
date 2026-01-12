package com.mopl.domain.contents.processor;

import com.mopl.domain.content.entity.Content;
import com.mopl.domain.content.entity.Tag;
import com.mopl.domain.content.enums.ContentType;
import com.mopl.domain.content.repository.TagRepository;
import com.mopl.domain.contents.dto.SportDbDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Stream;

import static com.mopl.domain.contents.dto.tmdb.KeywordDto.tagCache;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class SportProcessor implements ItemProcessor<SportDbDto, Content> {

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
    public Content process(SportDbDto item) {
        try{
            Content content = new Content(
                ContentType.sport,
                item.title(),
                item.description(),
                item.thumbnailUrl()
            );

            // 1. SportDbDto에서 명시한 세 개의 필드만 추출
            List<String> tagNames = Stream.of(item.strVenue(), item.strSport())
                    .filter(name -> name != null && !name.isBlank())
                    .distinct()
                    .toList();

            // 추출한 태그들을 순회하며 등록
            tagNames.forEach(tagName -> {
                Tag tag = tagCache.computeIfAbsent(tagName, name ->
                        tagRepository.findByTag(name)
                                .orElseGet(() -> tagRepository.save(new Tag(name)))
                );
                content.addTag(tag);
            });

            return content;

        } catch (Exception e) {
            log.error("TV 시리즈 상세 정보 처리 실패 - ID: {}, 사유: {}", item.id(), e.getMessage());
            return null; // 에러 발생 시 해당 아이템은 건너뜀
        }
    }
}
