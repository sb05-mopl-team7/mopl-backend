package com.mopl.domain.watching.dto.response;

import com.mopl.domain.user.dto.response.UserSummaryDto;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record WatchingSessionEventPayload(
        Long id, // userId
        LocalDateTime createdAt,
        UserSummaryDto watcher, // 전부 필요
        ContentPayload content // id만
){
}
