package com.mopl.domain.conversation.service;

import com.mopl.domain.conversation.dto.request.ConversationCreateRequest;
import com.mopl.domain.conversation.dto.response.ConversationDto;
import com.mopl.domain.conversation.entity.Conversation;
import com.mopl.domain.conversation.entity.DirectMessage;
import com.mopl.domain.conversation.entity.ReadStatus;
import com.mopl.domain.conversation.exception.ConversationErrorCode;
import com.mopl.domain.conversation.exception.ConversationException;
import com.mopl.domain.conversation.repository.ConversationRepository;
import com.mopl.domain.conversation.repository.DirectMessageRepository;
import com.mopl.domain.conversation.repository.ReadStatusRepository;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.exception.UserErrorCode;
import com.mopl.domain.user.exception.UserException;
import com.mopl.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Slf4j
@SpringBootTest
@Transactional
class ConversationServiceTest {

    @Autowired ConversationService conversationService;
    @Autowired UserRepository userRepository;
    @Autowired ConversationRepository conversationRepository;
    @Autowired ReadStatusRepository readStatusRepository;
    @Autowired DirectMessageRepository directMessageRepository;
    @Autowired EntityManager em; // 영속성 컨텍스트 제어용

    @Test
    @DisplayName("성공: 기존 대화방이 없으면 DB에 새로 생성하고 반환한다")
    void createConversation_New() {
        // given
        User me = userRepository.save(new User("me", "me@test.com", "pw"));
        User target = userRepository.save(new User("target", "target@test.com", "pw"));

        ConversationCreateRequest request = new ConversationCreateRequest(target.getId());

        // when
        ConversationDto result = conversationService.createConversation(me.getId(), request);

        // then
        // 반환값 검증
        assertThat(result.id()).isNotNull();
        assertThat(result.with().userId()).isEqualTo(target.getId());
        assertThat(result.lastestMessage()).isNull();
        assertThat(result.hasUnread()).isFalse();

        // 실제 DB 저장 확인
        List<ReadStatus> statuses = readStatusRepository.findAll();
        assertThat(statuses).hasSize(2);
    }

    @Test
    @DisplayName("성공: 이미 존재하는 대화방이 있으면 DB에서 찾아 반환한다")
    void createConversation_Existing() {
        // given
        User me = userRepository.save(new User("me", "me@test.com", "pw"));
        User target = userRepository.save(new User("target", "target@test.com", "pw"));

        // 기존 대화방 세팅
        Conversation conversation = conversationRepository.save(new Conversation());

        ReadStatus myStatus = readStatusRepository.save(ReadStatus.create(conversation, me));
        readStatusRepository.save(ReadStatus.create(conversation, target));

        // 메시지 전송 상황 세팅
        DirectMessage oldMessage = directMessageRepository.save(new DirectMessage(conversation, target, "이전 메시지"));
        myStatus.updateLastReadMsg(oldMessage);

        // 2. 상대방이 보낸 최신 메시지 (== 안 읽은 상태)
        directMessageRepository.save(new DirectMessage(conversation, target, "새로 보낸 메시지!!"));

        em.flush();
        em.clear();

        for (DirectMessage directMessage : directMessageRepository.findAll()) {
            log.info("대화방 {}: dm_id: {}, 작성자:{}, 내용: {}",directMessage.getConversation().getId() , directMessage.getId(), directMessage.getAuthor(), directMessage.getContent());
        }

        ConversationCreateRequest request = new ConversationCreateRequest(target.getId());

        // when
        ConversationDto result = conversationService.createConversation(me.getId(), request);

        // then
        assertThat(result.id()).isEqualTo(conversation.getId()); // 기존 대화방 반환하는지 확인
        assertThat(result.lastestMessage()).isNotNull();
        assertThat(result.lastestMessage().content()).isEqualTo("새로 보낸 메시지!!"); // 가장 최신에 온 메시지 확인
        assertThat(result.hasUnread()).isTrue();
    }

    @Test
    @DisplayName("실패: 자기 자신과 대화를 시도하면 예외가 발생한다")
    void createConversation_Self_Fail() {
        User me = userRepository.save(new User("me", "me@test.com", "pw"));
        ConversationCreateRequest request = new ConversationCreateRequest(me.getId());

        assertThatThrownBy(() -> conversationService.createConversation(me.getId(), request))
                .isInstanceOf(ConversationException.class)
                .hasFieldOrPropertyWithValue("errorCode", ConversationErrorCode.SELF_CONVERSATION_NOT_ALLOWED);
    }

    @Test
    @DisplayName("실패: 존재하지 않는 유저와 대화 시도")
    void createConversation_UserNotFound() {
        User me = userRepository.save(new User("me", "me@test.com", "pw"));
        Long unknownId = 9999L;
        ConversationCreateRequest request = new ConversationCreateRequest(unknownId);

        assertThatThrownBy(() -> conversationService.createConversation(me.getId(), request))
                .isInstanceOf(UserException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserErrorCode.USER_NOT_EXIST);
    }
}