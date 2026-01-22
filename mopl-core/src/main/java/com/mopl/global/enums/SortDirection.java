package com.mopl.global.enums;

import org.springframework.data.domain.Sort;

public enum SortDirection {
    ASCENDING, DESCENDING;

    public Sort.Direction toSpring() {
        return (this == ASCENDING) ? Sort.Direction.ASC : Sort.Direction.DESC;
    }
}

