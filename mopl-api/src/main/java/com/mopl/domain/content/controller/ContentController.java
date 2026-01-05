package com.mopl.domain.content.controller;

import com.mopl.domain.content.dto.ContentDto;
import com.mopl.domain.content.dto.CreateContentDto;
import com.mopl.domain.content.exception.ContentErrorCode;
import com.mopl.domain.content.exception.ContentException;
import com.mopl.domain.content.service.ContentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/contents")
public class ContentController {

    private final ContentService contentService;

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
}