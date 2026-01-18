package com.mopl.domain.notification.event;

import com.mopl.domain.notification.enums.NotificationType;

import java.util.List;

public record NotificationEvent(
        Long receiverId,
        NotificationType notificationType,
        List<String> args // 알림 메시지를 만들 때 사용되는 인자들. NotificationType 참고
) {
    public static NotificationEvent of(Long receiverId, NotificationType type, String... args) {
        return new NotificationEvent(receiverId, type, List.of(args));
    }
}
