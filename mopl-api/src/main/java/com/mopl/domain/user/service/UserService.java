package com.mopl.domain.user.service;

import com.mopl.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public Boolean existUser(String email) {
        return userRepository.findByEmail(email);
    }
}
