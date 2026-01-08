package com.mopl.domain.conversation.entity;

import com.mopl.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "conversations")
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class Conversation extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

}
