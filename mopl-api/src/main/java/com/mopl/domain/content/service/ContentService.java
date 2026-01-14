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
import com.mopl.global.dto.UploadFileRequest;
import com.mopl.global.s3.FileCategory;
import com.mopl.global.s3.S3Manager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentService {

    private final ContentRepository contentRepository;
    private final TagRepository tagRepository;
    private final S3Manager s3Manager;


    @Transactional
    public ContentDto create(CreateContentDto req, MultipartFile thumbnail) {
        log.info("콘텐츠 생성 요청: {}", req.title());

        String thumbnailUrl = uploadThumbnail(thumbnail); // 썸네일 업로드
        Content content = new Content(req.type(), req.title(), req.description(), thumbnailUrl); // 콘텐츠 생성
        addTagsToContent(req.tags(), content); // 콘텐츠에 태그 매핑
        Content newContent = contentRepository.save(content); // 저장

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

        if (thumbnail != null && !thumbnail.isEmpty()) {                        // 썸네일 변경사항이 있다면
            s3Manager.delete(content.getThumbnailUrl());                        // 기존 이미지 삭제
            String newThumbnailUrl = uploadThumbnail(thumbnail);                // S3에 새로운 썸네일 저장
            content.update(req.title(), req.description(), newThumbnailUrl);    // 새로운 썸네일로 업데이트
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
        Content content = getContentOrThrow(contentId); // 콘텐츠 조회
        s3Manager.delete(content.getThumbnailUrl());    // S3에서 썸네일 삭제
        contentRepository.deleteById(contentId);        // 콘텐츠 삭제
        log.info("콘텐츠 삭제 완료: id={}", contentId);
    }

    public ContentDto detail(Long contentId) {
        Content content = getContentOrThrow(contentId);
        List<String> tagNames = content.getContentTags().stream()
                .map(Contenttag -> Contenttag.getTag().getTag()).toList();
        int watchCount = 0; // TODO: redis에서 가져오기
        String thumbnailUrl = s3Manager.generatePresignedUrl(content.getThumbnailUrl());

        return new ContentDto(
                content.getId().toString(),
                content.getContentType(),
                content.getTitle(),
                content.getDescription(),
                thumbnailUrl,
                tagNames,
                content.getAverageRating(),
                content.getReviewCount(),
                watchCount
        );
    }

    /**
     * 콘텐츠 목록 조회
     * @param params 콘텐츠 조회 조건
     */
    @Transactional(readOnly = true)
    public PageResponse<ContentDto> list(ContentQueryParams params) {
        List<Content> contentList = contentRepository.list(params); // params 조건에 해당하는 콘텐츠 목록 조회

        boolean hasNext = contentList.size() > params.limit();
        String nextCursor = null;
        String nextAfter = null;

        if(hasNext) { // 다음 페이지가 존재하면
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
                    s3Manager.generatePresignedUrl(content.getThumbnailUrl()),
                    tagNames,
                    content.getAverageRating(),
                    content.getReviewCount(),
                    watchCount
            );
        }).toList();

        return PageResponse.<ContentDto> builder()
                .data(response)
                .nextCursor(nextCursor)
                .nextIdAfter(nextAfter)
                .hasNext(hasNext)
                .totalCount(0)
                .sortBy(params.sortBy())
                .sortDirection(params.sortDirection())
                .build();
    }

    private Content getContentOrThrow(Long contentId) {
        return contentRepository.findByIdWithTags(contentId)
            .orElseThrow(() -> new ContentException(ContentErrorCode.CONTENT_NOT_FOUND));
    }

    private String uploadThumbnail(MultipartFile thumbnail) {
        try {
            UploadFileRequest fileRequest = new UploadFileRequest(
                    thumbnail.getInputStream(),
                    thumbnail.getOriginalFilename(),
                    thumbnail.getSize(),
                    thumbnail.getContentType()
            );

            return s3Manager.upload(fileRequest, FileCategory.CONTENT_THUMBNAIL);
        } catch (IOException e) {
            log.error("썸네일 업로드 실패", e);
            throw new ContentException(ContentErrorCode.INVALID_THUMBNAIL);
        }
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
