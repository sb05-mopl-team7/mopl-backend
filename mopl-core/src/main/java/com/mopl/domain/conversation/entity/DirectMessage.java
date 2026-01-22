package com.mopl.domain.conversation.entity;

import com.mopl.domain.user.entity.User;
import com.mopl.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "direct_messages", indexes = {
        @Index(name = "idx_dm_conv_created", columnList = "conversation_id, created_at DESC")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DirectMessage extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(nullable = false)
    private String content;

    public DirectMessage(Conversation conversation, User author, String content) {
        this.conversation = conversation;
        this.author = author;
        this.content = content;
    }
}
