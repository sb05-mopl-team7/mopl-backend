package com.mopl.domain.contents.processor;

import com.mopl.domain.content.entity.Content;
import com.mopl.domain.content.entity.Tag;
import com.mopl.domain.content.enums.ContentType;
import com.mopl.domain.content.repository.TagRepository;
import com.mopl.domain.contents.dto.tmdb.KeywordDto;
import com.mopl.domain.contents.dto.tmdb.TmdbDetailDto;
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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

        if(processedMovieIds.containsKey(movieId)){
            log.debug("이미 처리된 영화 ID: {}", movieId);
            return null;
        }

        try {
            TmdbDetailDto movie = tmdbClient.getMovieDetails(movieId)
                    .filter(m -> m.description() != null && !m.description().isBlank()).block();

            String thumbnailUrl = getThumbnailUrl(movie.thumbnailUrl());

            Content content = new Content(
                ContentType.movie,
                movie.title(),
                movie.description(),
                thumbnailUrl
            );

            saveTag(movie.genres(), content);

            processedMovieIds.put(movieId, true);
            return content;

        } catch (Exception e) {
            log.error("영화 상세 정보 조회 실패 - ID: {}, 사유: {}", movieId, e.getMessage());
            return null; // null을 리턴하면 해당 아이템은 Writer로 넘어가지 않고 필터링됨
        }
    }

    private String getThumbnailUrl(String imageUrl) {
        String baseUrl = "https://image.tmdb.org/t/p/w500";
        // 웹에서 받은 이미지를 byte[]로 변환
        byte[] imageBytes = imageDownloadUtil.downloadImage(baseUrl + imageUrl);
        return s3Manager.uploadByte(imageBytes, imageUrl, FileCategory.CONTENT_THUMBNAIL);
    }

    private void saveTag(List<KeywordDto> genreList, Content content) {
        genreList.stream()
            .distinct()
            .forEach(genre -> {
                Tag tag = tagCache.get(genre.name());
                if (tag == null) {
                    tag = tagRepository.save(new Tag(genre.name()));
                    tagCache.put(genre.name(), tag);
                }
                content.addTag(tag);
            });
    }

    /**
     * 이미 처리한 TMDB 영화 ID를 저장하는 LRU 캐시입니다.
     * - 목적: 배치 실행 중 이미 처리한 TMDB 영화 ID 중복 방지
     * - 용량: 최대 50개 유지 (오래된 것부터 자동 제거)
     * - static: 배치 전역에서 공유
     * - 1~5 page
     */
    private static final Map<Long, Boolean> processedMovieIds = Collections.synchronizedMap(
        new LinkedHashMap<Long, Boolean>(50, 0.75f, true
    ) {
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return size() > 50;
            }
    });
}

