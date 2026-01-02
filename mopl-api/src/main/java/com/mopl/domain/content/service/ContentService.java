package com.mopl.domain.content.service;

import com.mopl.domain.content.dto.ContentDto;
import com.mopl.domain.content.dto.CreateContentDto;
import com.mopl.domain.content.entity.Content;
import com.mopl.domain.content.entity.Tag;
import com.mopl.domain.content.repository.ContentRepository;
import com.mopl.domain.content.repository.TagRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentService {

    private final ContentRepository contentRepository;
    private final TagRepository tagRepository;
    // private final S3Service s3Service; // TODO: S3 업로드 서비스 주입 필요

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ContentDto create(CreateContentDto req, MultipartFile thumbnail) {
        log.info("콘텐츠 생성 요청: {}", req.title());

        // 썸네일 업로드
        String thumbnailUrl = uploadThumbnail(thumbnail);
        // 콘텐츠 생성
        Content content = new Content(req.type(), req.title(), req.description(), thumbnailUrl);
        // 콘텐츠에 태그 매핑
        mapTagsToContent(req.tags(), content);
        // 저장
        Content newContent = contentRepository.save(content);

        List<String> tagNames = newContent.getContentTags().stream()
                                .map(Contenttag -> Contenttag.getTag().getTag()).toList();

        return ContentDto.builder()
                .id(newContent.getId())
                .title(newContent.getContentType().name())
                .title(newContent.getTitle())
                .thumbnailUrl(newContent.getThumbnailUrl())
                .tags(tagNames)
                .averageRating(newContent.getAverageRating())
                .reviewCount(newContent.getReviewCount())
                .watchCount(0)
                .build();

    }

    public void update() {
        // TODO: 콘텐츠 업데이트 로직 구현
    }

    public void delete() {
        // TODO: 콘텐츠 삭제 로직 구현
    }

    public void detail() {
        // TODO: 콘텐츠 단건 조회 로직 구현
    }

    public void list() {
        // TODO: 콘텐츠 목록조회 로직 구현
    }

    private String uploadThumbnail(MultipartFile thumbnail) {
        // TODO: 실제 S3Service.upload(thumbnail) 호출 로직으로 교체 필요
        // 지금은 임시 URL을 반환하도록 구성했음
        if (thumbnail == null || thumbnail.isEmpty()) {
            return null;
        }
        return "https://example.com/thumbnail.jpg"; // TODO: 임시 URL, S3 업로드 로직으로 교체 필요
    }

    private void mapTagsToContent(List<String> tagNames, Content content) {
        if (tagNames == null || tagNames.isEmpty()) return;

        tagNames.stream()
        .distinct()
        .forEach(tagName -> {
            Tag tag = tagRepository.findByTag(tagName)
                    .orElseGet(() -> tagRepository.save(new Tag(tagName)));

            content.addTag(tag);
        });
    }
}
