package com.mopl.domain.playlist.dto.request;

import jakarta.validation.constraints.Size;

public record PlaylistUpdateRequest(
        @Size(max = 255)
        String title,

        @Size(max = 255)
        String description
) {
}
