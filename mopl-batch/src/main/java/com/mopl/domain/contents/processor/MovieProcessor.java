package com.mopl.domain.contents.processor;

import com.mopl.domain.content.entity.Content;
import com.mopl.domain.content.entity.Tag;
import com.mopl.domain.content.enums.ContentType;
import com.mopl.domain.content.repository.TagRepository;
import com.mopl.domain.contents.openapi.TmdbClient;
import com.mopl.global.s3.FileCategory;
import com.mopl.global.s3.S3Manager;
import com.mopl.global.util.ImageDownloadUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.mopl.domain.contents.dto.tmdb.KeywordDto.tagCache;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class MovieProcessor implements ItemProcessor<Long, Content> {

    private final TmdbClient tmdbClient;
    private final TagRepository tagRepository;
    private final ImageDownloadUtil imageDownloadUtil;
    private final S3Manager s3Manager;

    @BeforeStep
    public void beforeStep(StepExecution stepExecution) {
        if (tagCache.isEmpty()) {
            List<Tag> allTags = tagRepository.findAll();
            allTags.forEach(tag -> tagCache.put(tag.getTag(), tag));
            log.info("태그 {}개를 캐시에 로드했습니다.", tagCache.size());
        }
    }

    @Override
    public Content process(Long movieId) throws Exception {
        try {
            return tmdbClient.getMovieDetails(movieId)
                    .filter(movie -> movie.description() != null && !movie.description().isBlank())
                    .map(movie -> {

                        // 웹에서 받은 이미지를 byte[]로 변환
                        byte[] imageBytes = imageDownloadUtil
                            .downloadImage("https://image.tmdb.org/t/p/w200" + movie.thumbnailUrl());
                        String imageName = movie.thumbnailUrl();
                        String thumbnailUrl = s3Manager.uploadByte(imageBytes, imageName, FileCategory.CONTENT_THUMBNAIL);

                        Content content = new Content(
                                ContentType.movie,
                                movie.title(),
                                movie.description(),
                                thumbnailUrl
                        );

                        // 태그 추가
                        movie.genres().stream()
                            .distinct()
                            .forEach(genre -> {
                                Tag tag = tagCache.computeIfAbsent(genre.name(), name -> {
                                    Tag newTag = new Tag(name);
                                    return tagRepository.save(newTag);
                                });
                                content.addTag(tag);
                            });

                        return content;
                })
                .block();
        } catch (Exception e) {
            log.error("영화 상세 정보 조회 실패 - ID: {}, 사유: {}", movieId, e.getMessage());
            return null; // null을 리턴하면 해당 아이템은 Writer로 넘어가지 않고 필터링됨
        }
    }
}
