package com.mopl.domain.content.service;

import com.mopl.domain.content.dto.ContentDto;
import com.mopl.domain.content.dto.ContentQueryParams;
import com.mopl.domain.content.dto.CreateContentDto;
import com.mopl.domain.content.dto.UpdateContentDto;
import com.mopl.domain.content.entity.Content;
import com.mopl.domain.content.entity.Tag;
import com.mopl.domain.content.exception.ContentErrorCode;
import com.mopl.domain.content.exception.ContentException;
import com.mopl.domain.content.repository.ContentRepository;
import com.mopl.domain.content.repository.TagRepository;
import com.mopl.global.dto.PageResponse;
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

        List<String> tagNames = content.getContentTags().stream()
                                .map(Contenttag -> Contenttag.getTag().getTag()).toList();

        return new ContentDto(
                newContent.getId().toString(),
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
                content.getId().toString(),
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

    public ContentDto detail(Long contentId) {
        Content content = getContentOrThrow(contentId);
        List<String> tagNames = content.getContentTags().stream()
                .map(Contenttag -> Contenttag.getTag().getTag()).toList();
        int watchCount = 0; // TODO: redis에서 가져오기

        return new ContentDto(
                content.getId().toString(),
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

    @Transactional(readOnly = true)
    public PageResponse<Object> list(ContentQueryParams params) {
        List<Content> contentList = contentRepository.list(params);

        boolean hasNext = contentList.size() > params.limit();
        String nextCursor = null;
        String nextAfter = null;

        if(hasNext) {
            Content lastContent = contentList.get(contentList.size() - 1);
            contentList.remove(lastContent);
            nextCursor = contentList.get(contentList.size()-1).getId().toString();
            nextAfter = contentList.get(contentList.size()-1).getCreatedAt().toString();
        }

        List<ContentDto> response = contentList.stream().map(content -> {
            int watchCount = 0; // TODO: redis에서 가져오기
            List<String> tagNames = content.getContentTags().stream()
                    .map(Contenttag -> Contenttag.getTag().getTag()).toList();

            return new ContentDto(
                    content.getId().toString(),
                    content.getContentType(),
                    content.getTitle(),
                    content.getDescription(),
                    content.getThumbnailUrl(),
                    tagNames,
                    content.getAverageRating(),
                    content.getReviewCount(),
                    watchCount
            );
        }).toList();

        List<Object> data = response.stream().map(dto -> (Object) dto).toList();

        return PageResponse.builder()
                .data(data)
                .nextCursor(nextCursor)
                .nextIdAfter(nextAfter)
                .hasNext(hasNext)
                .totalCount(0)
                .sortBy(params.sortBy())
                .sortDirection(params.sortDirection())
                .build();
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
