package com.mopl.domain.content.service;

import com.mopl.domain.content.dto.ContentDto;
import com.mopl.domain.content.dto.CreateContentDto;
import com.mopl.domain.content.dto.UpdateContentDto;
import com.mopl.domain.content.entity.Content;
import com.mopl.domain.content.entity.Tag;
import com.mopl.domain.content.exception.ContentErrorCode;
import com.mopl.domain.content.exception.ContentException;
import com.mopl.domain.content.repository.ContentRepository;
import com.mopl.domain.content.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
        addTagsToContent(req.tags(), content);
        // 저장
        Content newContent = contentRepository.save(content);

        List<String> tagNames = newContent.getContentTags().stream()
                                .map(Contenttag -> Contenttag.getTag().getTag()).toList();

        return new ContentDto(
                newContent.getId(),
                newContent.getContentType(),
                newContent.getTitle(),
                newContent.getDescription(),
                newContent.getThumbnailUrl(),
                tagNames,
                0,
                0,
                0
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ContentDto update(Long contentId, UpdateContentDto req, MultipartFile thumbnail) {
        Content content = getContentOrThrow(contentId);

        if (thumbnail != null && !thumbnail.isEmpty()) {
            String newThumbnailUrl = uploadThumbnail(thumbnail);
            content.update(req.title(), req.description(), newThumbnailUrl);
        } else {
            content.update(req.title(), req.description(), content.getThumbnailUrl());
        }

        addTagsToContent(req.tags(), content);
        List<String> tagNames = content.getContentTags().stream()
                .map(Contenttag -> Contenttag.getTag().getTag()).toList();

        int watchCount = 0; // TODO: redis에서 가져오기

        return new ContentDto(
                content.getId(),
                content.getContentType(),
                content.getTitle(),
                content.getDescription(),
                content.getThumbnailUrl(),
                tagNames,
                content.getAverageRating(),
                content.getReviewCount(),
                watchCount
        );
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Long contentId) {
        log.debug("콘텐츠 삭제 시작: id={}", contentId);
        if(!contentRepository.existsById(contentId)) {
            throw new ContentException(ContentErrorCode.CONTENT_NOT_FOUND);
        }
        contentRepository.deleteById(contentId);
        log.info("콘텐츠 삭제 완료: id={}", contentId);
    }

    public void detail() {
        // TODO: 콘텐츠 단건 조회 로직 구현
    }

    public void list() {
        // TODO: 콘텐츠 목록조회 로직 구현
    }

    private Content getContentOrThrow(Long contentId) {
        return contentRepository.findById(contentId)
            .orElseThrow(() -> new ContentException(ContentErrorCode.CONTENT_NOT_FOUND));
    }

    private String uploadThumbnail(MultipartFile thumbnail) {
        // TODO: 실제 S3Service.upload(thumbnail) 호출 로직으로 교체 필요
        // 지금은 임시 URL을 반환하도록 구성했음
        if (thumbnail == null || thumbnail.isEmpty()) {
            return null;
        }
        return "https://example.com/thumbnail.jpg"; // TODO: 임시 URL, S3 업로드 로직으로 교체 필요
    }

    private void addTagsToContent(List<String> tagNames, Content content) {
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
