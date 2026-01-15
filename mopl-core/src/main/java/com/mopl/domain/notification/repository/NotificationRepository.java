package com.mopl.domain.notification.repository;

import com.mopl.domain.notification.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    @Query("""
            SELECT n FROM Notification n
            WHERE n.receiverId = :userId
                    AND (:cursor IS NULL
                           OR n.createdAt < :cursor
                           OR (n.createdAt = :cursor AND n.id < :idAfter))
            ORDER BY n.createdAt DESC, n.id DESC
            """)
    List<Notification> findNotifications(
            @Param("userId") Long userId,
            @Param("cursor") LocalDateTime cursor,
            @Param("idAfter") Long idAfter,
            Pageable pageable
    );

    @Query("""
        select count(n)
        from Notification n
        where n.receiverId = :userId
        """)
    Long countByReceiverId(@Param("userId")Long userId);
}
