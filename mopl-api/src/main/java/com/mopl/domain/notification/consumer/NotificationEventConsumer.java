package com.mopl.domain.notification.consumer;

import com.mopl.domain.notification.event.NotificationEvent;
import com.mopl.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(topics = "${mopl.kafka.topics.noti}", groupId = "${mopl.kafka.consumer.noti-group-id}")
    public void consumeNotificationEvent(NotificationEvent event) {
        log.info("알림 소비: receiverId={}, type={}", event.receiverId(), event.notificationType());

        notificationService.create(event.receiverId(), event.notificationType(), event.args().toArray());
    }
}
