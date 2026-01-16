package com.mopl.domain.auth.service;

import com.mopl.domain.auth.dto.JwtDto;
import com.mopl.domain.auth.dto.TokenResultDto;
import com.mopl.domain.auth.exception.AuthErrorCode;
import com.mopl.domain.auth.exception.AuthException;
import com.mopl.domain.auth.jwt.JwtTokenProvider;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.exception.UserErrorCode;
import com.mopl.domain.user.exception.UserException;
import com.mopl.domain.user.mapper.UserMapper;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.global.redis.RedisManager;
import com.mopl.global.redis.RedisNameSpace;
import com.mopl.global.s3.S3Manager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;
    private final S3Manager s3Manager;
    private final RedisManager redisManager;

    public TokenResultDto login(String username, String password) {
        User user = userRepository.findByEmailAndLockedFalse(username)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_EXIST));
        //DB 비밀번호 일치
        if (passwordEncoder.matches(password, user.getPassword())) {
            return generateToken(user);
        }
        //임시 비밀번호 일치
        if (redisManager.hasKey(RedisNameSpace.TEMP_PASSWORD, username)) {
            Optional<String> tempPassword = redisManager.findByKey(
                    RedisNameSpace.TEMP_PASSWORD,
                    username,
                    String.class);
            if (tempPassword.isPresent() && tempPassword.get().equals(password)) {
                redisManager.delete(RedisNameSpace.TEMP_PASSWORD, username);
                return generateToken(user);
            }
        }
        //둘다 틀린 경우
        throw new UserException(UserErrorCode.PASSWORD_NOT_CORRECT);
    }

    private TokenResultDto generateToken(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user);

        String refreshToken = jwtTokenProvider.createRefreshToken(user);
        redisManager.save(RedisNameSpace.AUTH_TOKEN, user.getId().toString(), refreshToken);

        String thumbnailUrl = s3Manager.generatePresignedUrl(user.getProfileImageUrl());
        JwtDto jwtDto = new JwtDto(userMapper.toDto(user, thumbnailUrl), accessToken);

        return new TokenResultDto(jwtDto, refreshToken);
    }

    public void logout(Long myId) {
        redisManager.delete(RedisNameSpace.AUTH_TOKEN, myId.toString());
    }

    public TokenResultDto refresh(String refreshToken) {
        //jwt 유효 검증
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new AuthException(AuthErrorCode.REFRESH_TOKEN_INVALID);
        }

        Long userId = jwtTokenProvider.getUserIdFromRefreshToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_EXIST));

        Optional<String> token = redisManager.findByKey(
                RedisNameSpace.AUTH_TOKEN,
                userId.toString(),
                String.class);
        //refresh 값 불일치
        if (token.isEmpty() || !token.get().equals(refreshToken)) {
            throw new AuthException(AuthErrorCode.REFRESH_TOKEN_INVALID);
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(user);
        //새로운 refresh 토큰 redis에 저장
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user);
        redisManager.save(RedisNameSpace.AUTH_TOKEN, userId.toString(), newRefreshToken);

        String thumbnailUrl = s3Manager.generatePresignedUrl(user.getProfileImageUrl());
        JwtDto jwtDto = new JwtDto(userMapper.toDto(user, thumbnailUrl), newAccessToken);

        return new TokenResultDto(jwtDto, newRefreshToken);
    }
}
