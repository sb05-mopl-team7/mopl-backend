package com.mopl.domain.conversation.entity;

import com.mopl.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "read_status")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReadStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_message_id")
    private DirectMessage lastReadMessage;

    public static ReadStatus create(Conversation conversation, User user) {
        ReadStatus readStatus = new ReadStatus();
        readStatus.conversation = conversation;
        readStatus.user = user;
        return readStatus;
    }

    public void updateLastReadMsg(DirectMessage lastReadMessage) {
        this.lastReadMessage = lastReadMessage;
    }
}
