package com.mopl.domain.contents.dto.sportDb;

import java.util.List;

public record SportDbResponse<T>(
    List<T> events
) {}
