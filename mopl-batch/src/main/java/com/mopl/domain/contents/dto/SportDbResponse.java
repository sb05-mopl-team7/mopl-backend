package com.mopl.domain.contents.dto;

import java.util.List;

public record SportDbResponse<T>(
    List<T> events
) {}
