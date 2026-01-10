package com.mopl.domain.notification.dto;

import com.mopl.domain.notification.enums.Level;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.time.LocalDateTime;

public record NotificationDto(
        @JsonSerialize(using = ToStringSerializer.class)
        Long id,
        LocalDateTime createdAt,
        @JsonSerialize(using = ToStringSerializer.class)
        Long receiverId,
        String title,
        String content,
        Level level
) {
}
