package com.mopl.domain.user.service;

import com.mopl.domain.user.dto.UserCreateRequest;
import com.mopl.domain.user.dto.UserDto;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.exception.UserException;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.global.redis.RedisManager;
import com.mopl.global.s3.S3Manager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class UserServiceTest {

    @Autowired UserService userService;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired S3Manager s3Manager;      // 실제 빈 사용 - 수정 예정
    @Autowired RedisManager redisManager;  // 실제 빈 사용


    @Test
    @DisplayName("성공: 사용자 생성 시 DB에 저장되고 암호화된 비밀번호를 가진다")
    void createUser_Success () {
        // given
        UserCreateRequest request = new UserCreateRequest("사용자1","email@com.com", "password1234");

        // when
        UserDto result = userService.createUser(request);

        // then
        // 반환값 검증
        assertThat(result).isNotNull();
        assertThat(result.id()).isNotNull();  // ID가 생성되었는지
        assertThat(result.name()).isEqualTo("사용자1");
        assertThat(result.email()).isEqualTo("email@com.com");

        // 실제 DB 저장 확인
        User savedUser = userRepository.findById(result.id()).orElseThrow();
        assertThat(savedUser.getName()).isEqualTo("사용자1");
        assertThat(savedUser.getEmail()).isEqualTo("email@com.com");

        //비밀번호 암호화 확인
        assertThat(savedUser.getPassword()).isNotEqualTo("password1234");  // 암호화됨
        assertThat(savedUser.getPassword()).startsWith("$2a$");  // bcrypt 형식

    }

    @Test
    @DisplayName("실패: 중복 이메일로 가입 시 예외 발생")
    void createUser_Fail_DuplicateEmail() {
        // given
        userService.createUser(
                new UserCreateRequest("사용자1", "dup@test.com", "pw1234")
        );

        // when & then
        UserCreateRequest duplicateRequest = new UserCreateRequest(
                "사용자2",
                "dup@test.com",  // 같은 이메일
                "pw5678"
        );

        assertThatThrownBy(() ->
                userService.createUser(duplicateRequest)
        ).isInstanceOf(UserException.class);
    }



}
