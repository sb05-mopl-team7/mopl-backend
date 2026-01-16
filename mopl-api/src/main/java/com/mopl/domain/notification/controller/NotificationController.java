package com.mopl.domain.notification.controller;

import com.mopl.domain.auth.dto.UserPrincipal;
import com.mopl.domain.notification.dto.NotificationDto;
import com.mopl.domain.notification.service.NotificationService;
import com.mopl.global.dto.PageResponse;
import com.mopl.global.enums.SortDirection;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<PageResponse<NotificationDto>> findAll(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String idAfter,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer limit,
            @RequestParam(defaultValue = "DESCENDING") SortDirection sortDirection,
            @RequestParam(defaultValue = "createdAt") String sortBy
    ) {
        Long userId = userPrincipal.getUserId();
        return ResponseEntity.ok(notificationService.findAll(
                cursor, idAfter, limit, sortDirection, sortBy, userId
        ));
    }

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                       @PathVariable Long notificationId) {
        Long userId = userPrincipal.getUserId();
        notificationService.deleteNotification(userId,notificationId);
        return ResponseEntity.noContent().build();
    }
}
