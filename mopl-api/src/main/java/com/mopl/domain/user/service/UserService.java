package com.mopl.domain.user.service;

import com.mopl.domain.user.dto.UserCreateRequest;
import com.mopl.domain.user.dto.UserDto;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.exception.UserErrorCode;
import com.mopl.domain.user.exception.UserException;
import com.mopl.domain.user.mapper.UserMapper;
import com.mopl.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public Boolean existUser(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional
    public UserDto createUser(UserCreateRequest  dto) {
        if(existUser(dto.email())){
            throw new UserException(UserErrorCode.DUPLICATE_USER);
        }
        User user = new User(dto.name(),dto.email(),passwordEncoder.encode(dto.password()));
        User createdUser = userRepository.save(user);
        return userMapper.toDto(createdUser);
    }

    @Transactional(readOnly = true)
    public UserDto findById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new UserException(UserErrorCode.USER_NOT_EXIST));
        return  userMapper.toDto(user);
    }



}
