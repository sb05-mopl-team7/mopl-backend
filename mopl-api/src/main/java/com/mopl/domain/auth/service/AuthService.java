package com.mopl.domain.auth.service;

import com.mopl.domain.auth.dto.JwtDto;
import com.mopl.domain.auth.dto.TokenResultDto;
import com.mopl.domain.auth.exception.AuthErrorCode;
import com.mopl.domain.auth.exception.AuthException;
import com.mopl.domain.auth.jwt.JwtTokenProvider;
import com.mopl.domain.user.dto.response.UserSummaryDto;
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

    /**
     * 로그인
     * DB 비밀번호 또는 임시 비밀번호(Redis)로 인증합니다.
     */
    public TokenResultDto login(String email, String password) {
        User user = validateUser(email);
        return validatePassword(email, password, user);
    }

    public void logout(Long myId) {
        redisManager.delete(RedisNameSpace.AUTH_TOKEN, myId.toString());
    }

    /**
     * Refresh Token을 검증하고 새로운 Access/Refresh Token을 발급합니다.
     * Redis에 저장된 토큰과 비교하여 탈취된 토큰 사용을 방지합니다.
     */
    public TokenResultDto refresh(String refreshToken) {
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new AuthException(AuthErrorCode.REFRESH_TOKEN_INVALID);
        }

        Long userId = jwtTokenProvider.getUserIdFromRefreshToken(refreshToken);
        Optional<String> token = redisManager.findByKey(
                RedisNameSpace.AUTH_TOKEN,
                userId.toString(),
                String.class);
        if (token.isEmpty() || !token.get().equals(refreshToken)) {
            throw new AuthException(AuthErrorCode.REFRESH_TOKEN_INVALID);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_EXIST));
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user);
        String newAccessToken = jwtTokenProvider.createAccessToken(user);
        String thumbnailUrl = s3Manager.generatePresignedUrl(user.getProfileImageUrl());
        JwtDto jwtDto = new JwtDto(userMapper.toDto(user, thumbnailUrl), newAccessToken);

        redisManager.save(RedisNameSpace.AUTH_TOKEN, userId.toString(), newRefreshToken);
        UserSummaryDto userSummary =  new UserSummaryDto(user.getId(), user.getName(), thumbnailUrl);
        redisManager.save(RedisNameSpace.USER_SUMMARY, user.getId().toString(), userSummary);

        return new TokenResultDto(jwtDto, newRefreshToken);
    }

    /** 이메일 검증 */
    private User validateUser(String email) {
        return userRepository.findByEmailAndLockedFalse(email)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_EXIST));
    }

    /** 비밀번호 검증 */
    private TokenResultDto validatePassword(String email, String password, User user) {
        // 패스워드 일치할 경우
        if (passwordEncoder.matches(password, user.getPassword())) {
            return generateToken(user);
        }

        // 임시 패스워드로 로그인
        return redisManager.findByKey(RedisNameSpace.TEMP_PASSWORD, email, String.class)
                .filter(temp -> temp.equals(password))
                .map(temp -> {
                    redisManager.delete(RedisNameSpace.TEMP_PASSWORD, email);
                    return generateToken(user);
                })
                .orElseThrow(() -> new UserException(UserErrorCode.PASSWORD_NOT_CORRECT));
    }

    /** 로그인 성공 후 토큰 생성 */
    private TokenResultDto generateToken(User user) {
        String refreshToken = jwtTokenProvider.createRefreshToken(user);
        String accessToken = jwtTokenProvider.createAccessToken(user);
        String thumbnailUrl = s3Manager.generatePresignedUrl(user.getProfileImageUrl());
        JwtDto jwtDto = new JwtDto(userMapper.toDto(user, thumbnailUrl), accessToken);
        UserSummaryDto userSummary =  new UserSummaryDto(user.getId(), user.getName(), thumbnailUrl);

        redisManager.save(RedisNameSpace.AUTH_TOKEN, user.getId().toString(), refreshToken);
        redisManager.save(RedisNameSpace.USER_SUMMARY, user.getId().toString(), userSummary);

        return new TokenResultDto(jwtDto, refreshToken);
    }
}