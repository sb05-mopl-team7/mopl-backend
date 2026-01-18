package com.mopl.domain.notification.consumer;

import com.mopl.domain.notification.event.DmNotificationEvent;
import com.mopl.global.sse.SseManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationConsumer {

    private final SseManager sseManager;

    @KafkaListener(topics = "dm-notification", groupId = "api-server-group")
    public void consumeDmNotification(DmNotificationEvent event) {
        log.info("유저 {}의 DM 알림 소비", event.receiverId());

        sseManager.sendToUser(
                event.receiverId(),
                "direct-messages",
                event.directMessageDto()
        );
    }
}
