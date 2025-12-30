package com.mopl.domain.user.service;

import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User findById(UUID userId) {
        userRepository.findById(userId);
        return null;
    }
}
