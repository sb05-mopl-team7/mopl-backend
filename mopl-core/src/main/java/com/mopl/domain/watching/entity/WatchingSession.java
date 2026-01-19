package com.mopl.domain.watching.entity;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class WatchingSession {

    private Long id;
    private Long contentId;
    private LocalDateTime createdAt;

    public static WatchingSession create(Long id, Long contentId) {
        return WatchingSession.builder()
                .id(id)
                .contentId(contentId)
                .createdAt(LocalDateTime.now())
                .build();
    }
}