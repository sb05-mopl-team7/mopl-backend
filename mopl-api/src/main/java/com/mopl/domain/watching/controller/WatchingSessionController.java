package com.mopl.domain.watching.controller;

import com.mopl.domain.watching.controller.docs.WatchingSessionControllerDocs;
import com.mopl.domain.watching.dto.response.WatchingSessionContentListResponse;
import com.mopl.domain.watching.dto.response.WatchingSessionUserResponse;
import com.mopl.domain.watching.service.WatchingSessionService;
import com.mopl.global.enums.SortDirection;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class WatchingSessionController implements WatchingSessionControllerDocs {

    private final WatchingSessionService watchingSessionService;

    @Override
    public ResponseEntity<WatchingSessionUserResponse> getWatchingSession(
            @PathVariable("watcherId") Long watcherId
    ) {
        WatchingSessionUserResponse response = watchingSessionService.getWatchingSession(watcherId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<WatchingSessionContentListResponse> getWatchingSessionsByContent(
            @PathVariable("contentId") Long contentId,
            @RequestParam(value = "watcherNameLike", required = false) String watcherNameLike,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "idAfter", required = false) Long idAfter,
            @RequestParam(value = "limit", defaultValue = "10") Integer limit,
            @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortDirection", defaultValue = "DESCENDING") SortDirection sortDirection
    ) {
        WatchingSessionContentListResponse response = watchingSessionService.getWatchingSessionsByContent(
                contentId,
                watcherNameLike,
                cursor,
                idAfter,
                limit,
                sortBy,
                sortDirection
        );
        return ResponseEntity.ok(response);
    }
}