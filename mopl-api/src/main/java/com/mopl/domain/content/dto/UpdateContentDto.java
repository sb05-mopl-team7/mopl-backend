package com.mopl.domain.content.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record UpdateContentDto(
        @NotBlank(message = "제목은 비어있을 수 없습니다.")
        String title,
        @NotBlank(message = "설명은 비어있을 수 없습니다.")
        String description,
        List<String> tags
) {
}