package com.mopl.domain.contents.processor;

import com.mopl.domain.content.entity.Content;
import com.mopl.domain.content.entity.Tag;
import com.mopl.domain.content.enums.ContentType;
import com.mopl.domain.content.repository.TagRepository;
import com.mopl.domain.contents.dto.sportDb.SportDbDto;
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
import java.util.stream.Stream;

import static com.mopl.domain.contents.dto.tmdb.KeywordDto.tagCache;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class SportProcessor implements ItemProcessor<SportDbDto, Content> {

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
    public Content process(SportDbDto item) {

            String thumbnailUrl = getThumbnailUrl(item.thumbnailUrl());

            Content content = new Content(
                ContentType.sport,
                item.title(),
                item.description(),
                    thumbnailUrl
            );

            List<String> tagNames = getTagNames(item); // item에서 추출한 태그명
            saveTag(tagNames, content);

            return content;
    }

    /**
    * 웹에서 받은 이미지를 byte[]로 변환한 뒤 S3에 저장
    * @return S3에 저장된 이미지 URL
    * */
    private String getThumbnailUrl(String imageUrl) {
        byte[] imageBytes = imageDownloadUtil.downloadImage(imageUrl);
        String imageName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
        return s3Manager.uploadByte(imageBytes, imageName, FileCategory.CONTENT_THUMBNAIL);
    }

    /** 필요한 태그명만 추출 (null, 빈 문자열 제거) */
    private List<String> getTagNames(SportDbDto item) {
        return Stream.of(item.strVenue(), item.strSport())
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .toList();
    }

    /** 태그 저장 */
    private void saveTag(List<String> tagNames, Content content) {
        tagNames.forEach(tagName -> {
            Tag tag = tagCache.get(tagName);

            if (tag == null) {
                tag = tagRepository.save(new Tag(tagName)); // 새 태그 생성 + 즉시 저장
                tagCache.put(tagName, tag);                 // cache 갱신
            }

            content.addTag(tag);
        });
    }
}
