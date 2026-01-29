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
import com.mopl.global.redis.RedisManager;
import com.mopl.global.redis.RedisNameSpace;
import com.mopl.global.s3.FileCategory;
import com.mopl.global.s3.S3Manager;
import jakarta.persistence.EntityManager;
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
    private final RedisManager redisManager;
    private final EntityManager entityManager;

    @Transactional
    public ContentDto create(CreateContentDto req, MultipartFile thumbnail) {
        log.info("콘텐츠 생성 요청: {}", req.title());

        String thumbnailUrl = uploadThumbnail(thumbnail);                                           // S3에 썸네일 업로드
        Content content = new Content(req.type(), req.title(), req.description(), thumbnailUrl);    // 콘텐츠 생성
        addTagsToContent(req.tags(), content);                                                      // 태그 추가 및 저장

        Content newContent = contentRepository.save(content);
        List<String> tagNames = getTagNames(newContent);
        String presignedUrl = s3Manager.generatePresignedUrl(newContent.getThumbnailUrl());

        return toDto(newContent, presignedUrl, tagNames, 0);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ContentDto update(Long contentId, UpdateContentDto req, MultipartFile thumbnail) {
        Content content = validateContent(contentId);

        if (thumbnail != null && !thumbnail.isEmpty()) {                        // 썸네일 변경사항이 있다면
            String newThumbnailUrl = uploadThumbnail(thumbnail);                // 1. S3에 새로운 썸네일 저장
            content.update(req.title(), req.description(), newThumbnailUrl);    // 2. 새로운 썸네일로 업데이트
            s3Manager.delete(content.getThumbnailUrl());                        // 3. 기존 이미지 삭제
        } else {
            content.update(req.title(), req.description(), content.getThumbnailUrl());
        }

        content.getContentTags().clear();       // 메모리에서 태그 삭제
        entityManager.flush();                  // 실제 DB에서도 삭제
        addTagsToContent(req.tags(), content);  // 새로운 태그로 업데이트

        String presignedUrl = s3Manager.generatePresignedUrl(content.getThumbnailUrl());
        List<String> tagNames = getTagNames(content);
        int watchCount = getWatchCount(contentId);

        return toDto(content, presignedUrl, tagNames, watchCount);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Long contentId) {
        log.debug("콘텐츠 삭제 시작: id={}", contentId);
        Content content = validateContent(contentId); // 콘텐츠 조회
        s3Manager.delete(content.getThumbnailUrl());    // S3에서 썸네일 삭제
        contentRepository.deleteById(contentId);        // 콘텐츠 삭제
        // TODO: 콘텐츠에 작성된 리뷰 삭제
        log.info("콘텐츠 삭제 완료: id={}", contentId);
    }

    public ContentDto detail(Long contentId) {
        Content content = validateContent(contentId);
        String thumbnailUrl = s3Manager.generatePresignedUrl(content.getThumbnailUrl());
        List<String> tagNames = getTagNames(content);
        int watchCount = getWatchCount(contentId);

        return toDto(content, thumbnailUrl, tagNames, watchCount);
    }

    @Transactional(readOnly = true)
    public PageResponse<ContentDto> list(ContentQueryParams params) {
        List<Content> contentList = contentRepository.list(params); // params 조건에 해당하는 콘텐츠 목록 조회

        boolean hasNext = contentList.size() > params.limit();
        String nextCursor = null;
        String nextAfter = null;

        if(hasNext) { // 다음 페이지가 존재하면
            Content lastContent = contentList.get(contentList.size() - 1);
            contentList.remove(lastContent);
            switch (params.sortBy()) {
                case "watcherCount" -> nextCursor = String.valueOf(contentList.get(contentList.size()-1).getReviewCount());
                case "rate" -> nextCursor = String.valueOf(contentList.get(contentList.size()-1).getAverageRating());
                default -> nextCursor = contentList.get(contentList.size()-1).getId().toString();
            }
            nextAfter = contentList.get(contentList.size()-1).getId().toString();
        }

        List<ContentDto> response = contentList.stream().map(content -> {
            String presignedUrl = s3Manager.generatePresignedUrl(content.getThumbnailUrl());
            List<String> tagNames = getTagNames(content);
            int watchCount = getWatchCount(content.getId());

            return toDto(content, presignedUrl, tagNames, watchCount);
        })
        .toList();

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

    /** 콘텐츠 조회 및 검증 */
    private Content validateContent(Long contentId) {
        return contentRepository.findByIdWithTags(contentId)
            .orElseThrow(() -> new ContentException(ContentErrorCode.CONTENT_NOT_FOUND));
    }

    /** 썸네일 S3에 업로드 */
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

    /** 콘텐츠 태그 매핑 및 저장 */
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

    /** 콘텐츠에서 태그 이름만 추출 */
    private List<String> getTagNames(Content content) {
        return content.getContentTags().stream()
                .map(Contenttag -> Contenttag.getTag().getTag())
                .toList();
    }

    private int getWatchCount(Long contentId) {
        return (int) redisManager.getSetSize(RedisNameSpace.CONTENT_SESSIONS, String.valueOf(contentId));
    }

    /** Dto 변환 */
    private ContentDto toDto(Content content, String thumbnailUrl, List<String> tagNames, int watchCount) {
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
}
