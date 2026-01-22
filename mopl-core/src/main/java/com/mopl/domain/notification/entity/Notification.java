package com.mopl.domain.notification.entity;

import com.mopl.domain.notification.enums.Level;
import com.mopl.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "notifications", indexes = {
        @Index(name = "idx_noti_receiver_created", columnList = "receiver_id, created_at DESC")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 255)
    private String content;

    @Column(nullable = false, length = 255)
    @Enumerated(EnumType.STRING)
    private Level level;

    public Notification(Long receiverId, String title, String content, Level level) {
        this.receiverId = receiverId;
        this.title = title;
        this.content = content;
        this.level = level;
    }
}

