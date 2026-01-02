package com.mopl.domain.review.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReviewDto {

    private Long id;
    private Long contentId;
    private ReviewAuthorDto author;
    private String text;
    private double rating;
}
