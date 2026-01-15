package com.mopl.domain.watching.dto.response;

import com.mopl.domain.content.dto.ContentDto;
import com.mopl.domain.user.dto.response.UserSummaryDto;
import com.mopl.domain.watching.entity.WatchingSession;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.time.LocalDateTime;

@Builder
@Schema(description = "시청 세션 응답")
public record WatchingSessionResponse(
        @JsonSerialize(using = ToStringSerializer.class)
        @Schema(description = "세션 ID (watcherId와 동일)")
        String id,

        @Schema(description = "세션 생성 일시")
        LocalDateTime createdAt,

        @Schema(description = "시청자 정보")
        UserSummaryDto watcher, // UserSummaryDto 사용

        @Schema(description = "시청 중인 콘텐츠 정보")
        ContentDto content
) {
    public static WatchingSessionResponse of(WatchingSession session, UserSummaryDto watcher, ContentDto content) {
        return WatchingSessionResponse.builder()
                .id(String.valueOf(session.getId()))
                .createdAt(session.getCreatedAt())
                .watcher(watcher)
                .content(content)
                .build();
    }
}