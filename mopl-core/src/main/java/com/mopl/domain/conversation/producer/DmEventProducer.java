package com.mopl.domain.conversation.producer;

import com.mopl.domain.conversation.dto.response.DirectMessageDto;
import com.mopl.domain.conversation.event.DmSendEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DmEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${mopl.kafka.topics.dm}")
    private String dmTopic;

    public void send(Long receiverId, DirectMessageDto directMessageDto) {
        kafkaTemplate.send(dmTopic, new DmSendEvent(
                receiverId,
                directMessageDto
        ));
    }
}
