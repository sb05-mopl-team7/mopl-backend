package com.mopl.domain.notification.producer;

import com.mopl.domain.notification.enums.NotificationType;
import com.mopl.domain.notification.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${mopl.kafka.topics.noti}")
    private String notificationTopic;

    public void send(Long receiverId, NotificationType type, String... args) {
        NotificationEvent event = NotificationEvent.of(receiverId, type, args);
        kafkaTemplate.send(notificationTopic, event);
        log.info("알림 이벤트 발행: userId={}, type={}", receiverId, type);
    }
}
