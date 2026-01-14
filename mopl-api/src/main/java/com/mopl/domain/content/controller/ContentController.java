package com.mopl.domain.content.controller;

import com.mopl.domain.content.dto.ContentDto;
import com.mopl.domain.content.dto.ContentQueryParams;
import com.mopl.domain.content.dto.CreateContentDto;
import com.mopl.domain.content.dto.UpdateContentDto;
import com.mopl.domain.content.exception.ContentErrorCode;
import com.mopl.domain.content.exception.ContentException;
import com.mopl.domain.content.service.ContentService;
import com.mopl.global.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contents")
public class ContentController {

    private final ContentService contentService;

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "콘텐츠 생성")
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ContentDto> create(
            @RequestPart("request") @Valid CreateContentDto request,
            @RequestPart(value = "thumbnail") MultipartFile thumbnail
    ) {
        if (thumbnail == null || thumbnail.isEmpty()) {
            throw new ContentException(ContentErrorCode.INVALID_THUMBNAIL);
        }

        return ResponseEntity.ok(contentService.create(request, thumbnail));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "콘텐츠 수정")
    @PatchMapping(value = "/{contentId}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ContentDto> update(
            @PathVariable Long contentId,
            @RequestPart("request") @Valid UpdateContentDto request,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail
    ) {
        return ResponseEntity.ok(contentService.update(contentId, request, thumbnail));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "콘텐츠 삭제")
    @DeleteMapping(value = "/{contentId}")
    public ResponseEntity<Void> delete(@PathVariable Long contentId) {
        contentService.delete(contentId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "콘텐츠 단건 조회")
    @GetMapping(value = "/{contentId}")
    public ResponseEntity<ContentDto> detail(@PathVariable Long contentId) {
        return ResponseEntity.ok(contentService.detail(contentId));
    }

    @Operation(summary = "콘텐츠 목록 조회")
    @GetMapping
    public ResponseEntity<PageResponse<Object>> list(ContentQueryParams params) {
        return ResponseEntity.ok(contentService.list(params));
    }
}