package com.mopl.domain.watching.dto.response;

import com.mopl.domain.content.dto.ContentDto;
import com.mopl.domain.user.dto.response.UserSummaryDto;
import com.mopl.domain.watching.entity.WatchingSession;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.time.LocalDateTime;

@Builder
@Schema(description = "시청 세션 응답")
public record WatchingSessionUserResponse(
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "세션 ID (watcherId와 동일)")
        Long id,

        @Schema(description = "세션 생성 일시")
        LocalDateTime createdAt,

        @Schema(description = "시청자 정보")
        UserSummaryDto watcher,

        @Schema(description = "시청 중인 콘텐츠 정보")
        ContentDto content
) {
    public static WatchingSessionUserResponse of(WatchingSession session, UserSummaryDto watcher, ContentDto content) {
        if (session == null) return null;

        return WatchingSessionUserResponse.builder()
                .id(session.getId()) // 타입 변경에 따른 수정
                .createdAt(session.getCreatedAt())
                .watcher(watcher)
                .content(content)
                .build();
    }
}