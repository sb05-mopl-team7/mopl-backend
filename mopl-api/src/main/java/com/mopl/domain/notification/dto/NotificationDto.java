package com.mopl.domain.notification.dto;

import com.mopl.domain.notification.enums.Level;

import java.time.LocalDateTime;

public record NotificationDto(
        Long id,
        LocalDateTime createdAt,
        Long receiverId,
        String title,
        String content,
        Level level
) {
}
