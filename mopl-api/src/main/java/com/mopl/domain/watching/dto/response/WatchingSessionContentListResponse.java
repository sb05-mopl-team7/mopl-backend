package com.mopl.domain.watching.dto.response;

import com.mopl.global.enums.SortDirection;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.Collections;
import java.util.List;

@Builder
@Schema(description = "시청 세션 목록 조회 응답 (페이지네이션)")
public record WatchingSessionContentListResponse(
        @Schema(description = "시청 세션 목록 데이터")
        List<WatchingSessionUserResponse> data,

        @Schema(description = "다음 페이지 조회를 위한 커서 (createdAt 기반)")
        String nextCursor,

        @JsonSerialize(using = ToStringSerializer.class) // JS 정밀도 문제 방지 추가
        @Schema(description = "다음 페이지 조회를 위한 보조 커서 (watcherId 기반)")
        Long nextIdAfter,

        @Schema(description = "다음 페이지 존재 여부")
        boolean hasNext,

        @Schema(description = "전체 아이템 개수")
        long totalCount,

        @Schema(description = "정렬 기준")
        String sortBy,

        @Schema(description = "정렬 방향 (ASCENDING, DESCENDING)")
        SortDirection sortDirection
) {
    public static WatchingSessionContentListResponse empty(String sortBy, SortDirection sortDirection) {
        return WatchingSessionContentListResponse.builder()
                .data(Collections.emptyList())
                .nextCursor(null)
                .nextIdAfter(null)
                .hasNext(false)
                .totalCount(0)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();
    }
}