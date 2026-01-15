package com.mopl.domain.notification.service;


import com.mopl.domain.notification.dto.NotificationDto;
import com.mopl.domain.notification.entity.Notification;
import com.mopl.domain.notification.enums.Level;
import com.mopl.domain.notification.repository.NotificationRepository;
import com.mopl.global.dto.PageResponse;
import com.mopl.global.enums.SortDirection;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.global.sse.SseManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SseManager sseManager;

    @Transactional
    public void createAndSendNotification(
            Long receiverId,
            String title,
            String content,
            Level level
    ){
        Notification notification = new Notification(receiverId,title,content,level);
        notificationRepository.save(notification);

        NotificationDto notificationDto = toDto(notification);
        sseManager.sendToUser(
                receiverId,
                "notification",
                notificationDto
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationDto> findAll(
            String cursor,
            String idAfter,
            Integer limit,
            SortDirection sortDirection,
            String sortBy,
            Long userId
    ) {
        StartId key = parseStartId(cursor, idAfter);
        Pageable pageable = PageRequest.of(0, limit + 1);

        // 2. limit + 1개 조회하여 hasNext 판단
        List<Notification> fetched = notificationRepository.findNotifications(
                userId,
                key.cursorCreatedAt,
                key.idAfter,
                pageable
        );

        Long totalCount = notificationRepository.countByReceiverId(userId);

        boolean hasNext = fetched.size() > limit;

        // 4. 실제 반환할 데이터 (limit 개수만큼만)
        List<Notification> page = hasNext
                ? fetched.subList(0, limit)
                : fetched;

        // 5. DTO 변환
        List<NotificationDto> data = page.stream()
                .map(this::toDto)
                .toList();

        // 6. nextCursor와 nextIdAfter 계산
        String nextCursor = null;
        String nextIdAfter = null;

        if (hasNext && !page.isEmpty()) {
            Notification last = page.get(page.size() - 1);
            nextIdAfter = String.valueOf(last.getId());
            nextCursor = formatCreatedAtCursor(last.getCreatedAt());
        }


        // 8. 응답 생성
        return  PageResponse.<NotificationDto>builder()
                .data(data != null ? data : List.of())
                .nextCursor(nextCursor)
                .nextIdAfter(nextIdAfter)
                .hasNext(hasNext)
                .totalCount(totalCount)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();
    }

    private StartId parseStartId(String cursorRaw, String idAfterRaw) {
        boolean hasCursor = cursorRaw != null && !cursorRaw.isBlank();
        boolean hasIdAfter = idAfterRaw != null && !idAfterRaw.isBlank();

        if (hasCursor != hasIdAfter) {
            throw new MoplException(ErrorCode.INVALID_REQUEST);
        }

        if (!hasCursor) {
            return new StartId(null, null);
        }

        return new StartId(parseCreatedAtCursor(cursorRaw), parseLong(idAfterRaw));
    }

    private LocalDateTime parseCreatedAtCursor(String cursor) {
        String normalized = cursor.trim().replace(" ", "T");
        try {
            return LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            throw new MoplException(ErrorCode.INVALID_REQUEST);
        }
    }

    private Long parseLong(String idAfter) {
        try {
            return Long.parseLong(idAfter.trim());
        } catch (Exception e) {
            throw new MoplException(ErrorCode.INVALID_REQUEST);
        }

    }

    private String formatCreatedAtCursor(LocalDateTime createdAt) {
        return createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private NotificationDto toDto(Notification notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getCreatedAt(),
                notification.getReceiverId(),
                notification.getTitle(),
                notification.getContent(),
                notification.getLevel()
        );
    }

    private record StartId(LocalDateTime cursorCreatedAt, Long idAfter) {

    }
}