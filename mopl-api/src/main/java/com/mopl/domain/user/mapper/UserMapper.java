package com.mopl.domain.user.mapper;

import com.mopl.domain.user.dto.UserResponse;
import com.mopl.domain.user.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toUserResponse(User user);
}
