package com.mopl.domain.user.mapper;

import com.mopl.domain.user.dto.UserDto;
import com.mopl.domain.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    public abstract UserDto toDto(User user);

    // 프로필 이미지 URL 커스텀 매핑
    @Mapping(source = "thumbnailUrl", target = "profileImageUrl")
    public abstract UserDto toDto(User user, String thumbnailUrl);

    List<UserDto> toDtoList(List<User> users);
}
