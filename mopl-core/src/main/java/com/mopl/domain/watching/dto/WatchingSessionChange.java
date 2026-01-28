package com.mopl.domain.watching.dto;

import com.mopl.domain.watching.dto.response.WatchingSessionEventPayload;
import com.mopl.domain.watching.enums.ChangeType;

public record WatchingSessionChange(
        ChangeType type,
        WatchingSessionEventPayload watchingSession,
        long watcherCount
) {
}
