package com.mopl.domain.conversation.dto.request;

import com.mopl.global.enums.SortDirection;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 대화 목록 조회시 최근 메시지 생성일 내림차순 정렬하고
 * 메시지가 없는 대화방은 대화방 생성일 내림차순으로 정렬하기 때문에 하기 두 파라미터는 쿼리에서 사용하지 않음
 * @param sortDirection 사용 안 함
 * @param sortBy 사용 안 함
 */
public record ConversationSearchCondition(
        String keywordLike,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime cursor,

        @Positive(message = "유효한 값을 입력해주세요.")
        Long idAfter,

        @NotNull(message = "limit 값은 필수입니다.")
        @Min(value = 1, message = "조회 개수는 1 이상이어야 합니다.")
        @Max(value = 50, message = "조회 개수는 50을 초과할 수 없습니다.")
        Integer limit,

        SortDirection sortDirection,
        String sortBy
) {
        public ConversationSearchCondition {
                sortBy = "createdAt";
                sortDirection = SortDirection.DESCENDING;
        }
}
