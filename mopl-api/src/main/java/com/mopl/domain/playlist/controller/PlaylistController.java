package com.mopl.domain.playlist.controller;

import com.mopl.domain.playlist.controller.docs.PlaylistControllerDocs;
import com.mopl.domain.playlist.dto.request.PlaylistCreateRequest;
import com.mopl.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.mopl.domain.playlist.dto.response.PlaylistDto;
import com.mopl.domain.playlist.service.PlaylistService;
import com.mopl.global.dto.PageResponse;
import com.mopl.global.enums.SortDirection;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/playlists")
public class PlaylistController implements PlaylistControllerDocs {

    private final PlaylistService playlistService;

    // 1) GET /api/playlists
    @GetMapping
    @Override
    public ResponseEntity<PageResponse<PlaylistDto>> findAll(
            @RequestHeader(value = "X-User-Id", required = false) Long requesterId,
            @RequestParam(required = false) String keywordLike,
            @RequestParam(required = false) Long ownerIdEqual,
            @RequestParam(required = false) Long subscriberIdEqual,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String idAfter,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) Integer limit,
            @RequestParam(defaultValue = "DESCENDING") SortDirection sortDirection,
            @RequestParam(defaultValue = "updatedAt") String sortBy
    ) {
        return ResponseEntity.ok(
                playlistService.findAll(
                        requesterId, keywordLike, ownerIdEqual, subscriberIdEqual,
                        cursor, idAfter, limit, sortBy, sortDirection
                )
        );
    }

    // 2) POST /api/playlists
    @PostMapping
    @Override
    public ResponseEntity<PlaylistDto> create(
            @RequestHeader(value = "X-User-Id", required = false) Long requesterId,
            @RequestBody @Valid PlaylistCreateRequest request
    ) {
        PlaylistDto response = playlistService.create(requesterId, request);
        return ResponseEntity.status(201).body(response);
    }

    // 3) POST /api/playlists/{playlistId}/subscription
    @PostMapping("/{playlistId}/subscription")
    @Override
    public ResponseEntity<Void> subscribe(
            @RequestHeader(value = "X-User-Id", required = false) Long requesterId,
            @PathVariable Long playlistId
    ) {
        playlistService.subscribe(requesterId, playlistId);
        return ResponseEntity.noContent().build();
    }

    // 4) DELETE /api/playlists/{playlistId}/subscription
    @DeleteMapping("/{playlistId}/subscription")
    @Override
    public ResponseEntity<Void> unsubscribe(
            @RequestHeader(value = "X-User-Id", required = false) Long requesterId,
            @PathVariable Long playlistId
    ) {
        playlistService.unsubscribe(requesterId, playlistId);
        return ResponseEntity.noContent().build();
    }

    // 5) POST /api/playlists/{playlistId}/contents/{contentId}
    @PostMapping("/{playlistId}/contents/{contentId}")
    @Override
    public ResponseEntity<Void> addContent(
            @RequestHeader(value = "X-User-Id", required = false) Long requesterId,
            @PathVariable Long playlistId,
            @PathVariable Long contentId
    ) {
        playlistService.addContent(requesterId, playlistId, contentId);
        return ResponseEntity.noContent().build();
    }

    // 6) DELETE /api/playlists/{playlistId}/contents/{contentId}
    @DeleteMapping("/{playlistId}/contents/{contentId}")
    @Override
    public ResponseEntity<Void> removeContent(
            @RequestHeader(value = "X-User-Id", required = false) Long requesterId,
            @PathVariable Long playlistId,
            @PathVariable Long contentId
    ) {
        playlistService.removeContent(requesterId, playlistId, contentId);
        return ResponseEntity.noContent().build();
    }

    // 7) GET /api/playlists/{playlistId}
    @GetMapping("/{playlistId}")
    @Override
    public ResponseEntity<PlaylistDto> find(
            @RequestHeader(value = "X-User-Id", required = false) Long requesterId,
            @PathVariable Long playlistId
    ) {
        return ResponseEntity.ok(playlistService.find(requesterId, playlistId));
    }

    // 8) DELETE /api/playlists/{playlistId}
    @DeleteMapping("/{playlistId}")
    @Override
    public ResponseEntity<Void> delete(
            @RequestHeader(value = "X-User-Id", required = false) Long requesterId,
            @PathVariable Long playlistId
    ) {
        playlistService.delete(requesterId, playlistId);
        return ResponseEntity.noContent().build();
    }

    // 9) PATCH /api/playlists/{playlistId}
    @PatchMapping("/{playlistId}")
    @Override
    public ResponseEntity<PlaylistDto> update(
            @RequestHeader(value = "X-User-Id", required = false) Long requesterId,
            @PathVariable Long playlistId,
            @RequestBody @Valid PlaylistUpdateRequest request
    ) {
        return ResponseEntity.ok(playlistService.update(requesterId, playlistId, request));
    }
}
