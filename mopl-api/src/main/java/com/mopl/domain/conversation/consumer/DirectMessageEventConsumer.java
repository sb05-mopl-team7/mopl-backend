package com.mopl.domain.conversation.consumer;

import com.mopl.domain.conversation.event.DmSendEvent;
import com.mopl.global.sse.SseManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DirectMessageEventConsumer {

    private final SseManager sseManager;

    @KafkaListener(topics = "${mopl.kafka.topics.dm}", groupId = "${mopl.kafka.consumer.dm-group-id}")
    public void consumeDmEvent(DmSendEvent event) {
        log.info("유저 {}에 전달될 DM 소비", event.receiverId());

        sseManager.sendToUser(
                event.receiverId(),
                "direct-messages",
                event.directMessageDto()
        );
    }
}
