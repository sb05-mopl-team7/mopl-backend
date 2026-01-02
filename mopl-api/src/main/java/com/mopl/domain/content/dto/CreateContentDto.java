package com.mopl.domain.content.dto;

import com.mopl.domain.content.enums.ContentType;

import java.util.List;

public record CreateContentDto(
    ContentType type,
    String title,
    String description,
    List<String> tags
) {
}
