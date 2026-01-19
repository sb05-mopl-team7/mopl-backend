package com.mopl.domain.contents.processor;

import com.mopl.domain.content.entity.Content;
import com.mopl.domain.content.entity.Tag;
import com.mopl.domain.content.enums.ContentType;
import com.mopl.domain.content.repository.TagRepository;
import com.mopl.domain.contents.dto.tmdb.KeywordDto;
import com.mopl.domain.contents.dto.tmdb.TvSeriesDto;
import com.mopl.domain.contents.openapi.TmdbClient;
import com.mopl.global.s3.FileCategory;
import com.mopl.global.s3.S3Manager;
import com.mopl.global.util.ImageDownloadUtil;
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
    private final ImageDownloadUtil imageDownloadUtil;
    private final S3Manager s3Manager;

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

        if(item.description() == null || item.description().isBlank()) {
            log.info("상세 정보가 없어 처리를 건너뜁니다. - ID: {} title: {}", item.id(), item.title());
            return null;
        }

        try {
            String thumbnailUrl = getThumbnailUrl(item.thumbnailUrl());

            Content content = new Content(
                    ContentType.tvSeries,
                    item.id(),
                    item.title(),
                    item.description(),
                    thumbnailUrl
            );
            return tmdbClient.getTvSeriesKeyword(item.id())
                    .map(genreList -> {
                        saveTag(genreList, content);    // 태그 저장
                        return content;
                    })
                    .block(); // 동기 처리를 위해 block 사용
        } catch (Exception e) {
            log.error("TV 시리즈 정보 조회 실패 - ID: {}, 사유: {}", item.title(), e.getMessage());
            return null;
        }
    }

    /**
     * 웹에서 받은 이미지를 byte[]로 변환한 뒤 S3에 저장
     * @return S3에 저장된 이미지 URL
     * */
    private String getThumbnailUrl(String imageUrl) {
        String baseUrl = "https://image.tmdb.org/t/p/w500";
        byte[] imageBytes = imageDownloadUtil.downloadImage(baseUrl + imageUrl);
        return s3Manager.uploadByte(imageBytes, imageUrl, FileCategory.CONTENT_THUMBNAIL);
    }

    /** 태그 저장 */
    private void saveTag(List<KeywordDto> genreList, Content content) {
        genreList.forEach(genre -> {
            Tag tag = tagCache.get(genre.name());
            if (tag == null) {
                tag = tagRepository.save(new Tag(genre.name()));
                tagCache.put(genre.name(), tag);
            }
            content.addTag(tag);
        });
    }
}