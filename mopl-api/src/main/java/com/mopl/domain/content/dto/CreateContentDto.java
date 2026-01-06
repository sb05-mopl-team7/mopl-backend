package com.mopl.domain.content.dto;

import com.mopl.domain.content.enums.ContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateContentDto(
    @NotNull(message = "콘텐츠 타입은 필수입니다.")
    ContentType type,
    @NotBlank(message = "제목은 비어있을 수 없습니다.")
    String title,
    @NotBlank(message = "설명은 비어있을 수 없습니다.")
    String description,
    List<String> tags
) {
}
