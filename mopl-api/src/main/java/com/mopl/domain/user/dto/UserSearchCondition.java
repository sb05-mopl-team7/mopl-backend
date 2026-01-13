package com.mopl.domain.user.dto;

import com.mopl.domain.user.enums.Role;
import com.mopl.global.enums.SortBy;
import com.mopl.global.enums.SortDirection;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UserSearchCondition (
        String emailLike,
        Role roleEqual,
        Boolean isLocked,
        String cursor,
        String idAfter,
        @NotNull(message = "limit 값은 필수입니다.")
        @Min(value = 1, message = "조회 개수는 1 이상이어야 합니다.")
        @Max(value = 50, message = "조회 개수는 50을 초과할 수 없습니다.")
        Integer limit,
        SortDirection sortDirection,
        SortBy sortBy
){
    public UserSearchCondition{
        if (sortDirection == null) {
            sortDirection = SortDirection.ASCENDING;
        }
        if (sortBy == null) {
            sortBy = SortBy.name;
        }
    }
}
