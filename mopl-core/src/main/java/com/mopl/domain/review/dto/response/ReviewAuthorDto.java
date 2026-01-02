package com.mopl.domain.review.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReviewAuthorDto {

    private Long userId;
    private String name;
    private String profileImageUrl;
}
