package com.mopl.domain.playlist.controller;

import com.mopl.domain.auth.dto.UserPrincipal;
import com.mopl.domain.playlist.controller.docs.PlaylistControllerDocs;
import com.mopl.domain.playlist.dto.request.PlaylistCreateRequest;
import com.mopl.domain.playlist.dto.request.PlaylistUpdateRequest;
import com.mopl.domain.playlist.dto.response.PlaylistDto;
import com.mopl.domain.playlist.service.PlaylistService;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.global.dto.PageResponse;
import com.mopl.global.enums.SortDirection;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/playlists")
@Validated
public class PlaylistController implements PlaylistControllerDocs {

    private final PlaylistService playlistService;
    private final UserRepository userRepository;

    @Override
    @GetMapping
    public ResponseEntity<PageResponse<PlaylistDto>> findAll(
            Principal principal,
            @RequestParam(required = false) String keywordLike,
            @RequestParam(required = false) Long ownerIdEqual,
            @RequestParam(required = false) Long subscriberIdEqual,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) String idAfter,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) SortDirection sortDirection,
            @RequestParam(required = false) String sortBy
    ) {
        Long requesterId = resolveRequesterId(principal);

        return ResponseEntity.ok(
                playlistService.findAll(
                        requesterId,
                        keywordLike,
                        ownerIdEqual,
                        subscriberIdEqual,
                        cursor,
                        idAfter,
                        limit,
                        sortBy,
                        sortDirection
                )
        );
    }

    @Override
    @PostMapping
    public ResponseEntity<PlaylistDto> create(
            Principal principal,
            @Valid @RequestBody PlaylistCreateRequest request
    ) {
        Long requesterId = resolveRequesterId(principal);
        return ResponseEntity.ok(playlistService.create(requesterId, request));
    }

    @Override
    @PostMapping("/{playlistId}/subscription")
    public ResponseEntity<Void> subscribe(Principal principal, @PathVariable Long playlistId) {
        Long requesterId = resolveRequesterId(principal);
        playlistService.subscribe(requesterId, playlistId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @DeleteMapping("/{playlistId}/subscription")
    public ResponseEntity<Void> unsubscribe(Principal principal, @PathVariable Long playlistId) {
        Long requesterId = resolveRequesterId(principal);
        playlistService.unsubscribe(requesterId, playlistId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/{playlistId}/contents/{contentId}")
    public ResponseEntity<Void> addContent(
            Principal principal,
            @PathVariable Long playlistId,
            @PathVariable Long contentId
    ) {
        Long requesterId = resolveRequesterId(principal);
        playlistService.addContent(requesterId, playlistId, contentId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @DeleteMapping("/{playlistId}/contents/{contentId}")
    public ResponseEntity<Void> removeContent(
            Principal principal,
            @PathVariable Long playlistId,
            @PathVariable Long contentId
    ) {
        Long requesterId = resolveRequesterId(principal);
        playlistService.removeContent(requesterId, playlistId, contentId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/{playlistId}")
    public ResponseEntity<PlaylistDto> find(Principal principal, @PathVariable Long playlistId) {
        Long requesterId = resolveRequesterId(principal);
        return ResponseEntity.ok(playlistService.find(requesterId, playlistId));
    }

    @Override
    @DeleteMapping("/{playlistId}")
    public ResponseEntity<Void> delete(Principal principal, @PathVariable Long playlistId) {
        Long requesterId = resolveRequesterId(principal);
        playlistService.delete(requesterId, playlistId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping("/{playlistId}")
    public ResponseEntity<PlaylistDto> update(
            Principal principal,
            @PathVariable Long playlistId,
            @Valid @RequestBody PlaylistUpdateRequest request
    ) {
        Long requesterId = resolveRequesterId(principal);
        return ResponseEntity.ok(playlistService.update(requesterId, playlistId, request));
    }

    private Long resolveRequesterId(Principal principal) {
        if (principal == null) {
            throw new MoplException(ErrorCode.UNAUTHORIZED);
        }
        if (principal instanceof Authentication authentication) {
            Object authPrincipal = authentication.getPrincipal();
            if (authPrincipal instanceof UserPrincipal userPrincipal) {
                return userPrincipal.getUserId();
            }
        }

        String name = principal.getName();
        if (name == null || name.isBlank()) {
            throw new MoplException(ErrorCode.UNAUTHORIZED);
        }

        boolean isNumeric = name.chars().allMatch(Character::isDigit);
        if (isNumeric) {
            return Long.parseLong(name);
        }

        User user = userRepository.findByEmail(name)
                .orElseThrow(() -> new MoplException(ErrorCode.UNAUTHORIZED));
        return user.getId();
    }
}