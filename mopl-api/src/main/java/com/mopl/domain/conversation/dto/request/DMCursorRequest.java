package com.mopl.domain.conversation.dto.request;

import com.mopl.global.enums.SortDirection;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

public record DMCursorRequest(
        @NotNull(message = "대화방 ID는 필수값입니다.")
        @Positive(message = "대화방 ID는 양수여야 합니다.")
        Long conversationId,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime cursor,

        @Positive(message = "메시지 ID는 양수여야 합니다.")
        Long idAfter,

        @Min(value = 1, message = "조회 개수는 1 이상이어야 합니다.")
        @Max(value = 100, message = "한 번에 최대 100개까지만 조회할 수 있습니다.")
        Integer limit,

        SortDirection sortDirection, // 현재 DM 목록 조회 시 내림차순 고정

        @Pattern(regexp = "^(createdAt)$", message = "정렬 기준은 'createdAt'만 가능합니다.")
        String sortBy // 현재 DM 목록 조회 시 createdAt 고정
) {
    public DMCursorRequest {
        if (limit == null) {
            limit = 20;
        }
        if (sortDirection == null) {
            sortDirection = SortDirection.DESCENDING;
        }
        if (sortBy == null) {
            sortBy = "createdAt";
        }
    }
}
