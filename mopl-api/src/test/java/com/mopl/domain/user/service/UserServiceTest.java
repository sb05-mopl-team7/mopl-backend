package com.mopl.domain.user.service;

import com.mopl.domain.notification.enums.NotificationType;
import com.mopl.domain.notification.producer.NotificationEventProducer;
import com.mopl.domain.user.dto.UserCreateRequest;
import com.mopl.domain.user.dto.UserDto;
import com.mopl.domain.user.dto.UserSearchCondition;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.enums.Role;
import com.mopl.domain.user.exception.UserException;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.global.dto.PageResponse;
import com.mopl.global.dto.UploadFileRequest;
import com.mopl.global.enums.SortBy;
import com.mopl.global.enums.SortDirection;
import com.mopl.global.s3.FileCategory;
import com.mopl.global.s3.S3Manager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class UserServiceTest {

    @Autowired
    UserService userService;
    @Autowired
    UserRepository userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @MockitoBean
    private S3Manager s3Manager;
    @MockitoBean
    private NotificationEventProducer notificationEventProducer;

    // 테스트 데이터 생성 헬퍼
    private void createTestUsers() {
        userRepository.save(new User("Alice", "alice@test.com", "pw"));
        userRepository.save(new User("Bob", "bob@test.com", "pw"));
        userRepository.save(new User("Charlie", "charlie@test.com", "pw"));
        userRepository.save(new User("David", "david@test.com", "pw"));
        userRepository.save(new User("Eve", "eve@test.com", "pw"));
        userRepository.save(new User("Frank", "frank@test.com", "pw"));
        userRepository.save(new User("Grace", "grace@test.com", "pw"));
        userRepository.save(new User("Henry", "henry@test.com", "pw"));
        userRepository.save(new User("Iris", "iris@test.com", "pw"));
        userRepository.save(new User("Jack", "jack@test.com", "pw"));
    }


    @Test
    @DisplayName("성공: 사용자 생성 시 DB에 저장되고 암호화된 비밀번호를 가진다")
    void createUser_Success() {
        // given
        UserCreateRequest request = new UserCreateRequest("사용자1", "email@com.com", "password1234");

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

    @Test
    @DisplayName("성공: 사용자의 권한을 변경 한다(admin 계정 확인은 컨트롤러)")
    void updateRole_Success() {
        //given
        User me = userRepository.save(new User("me", "me@test.com", "pw"));
        Role oldRole = me.getRole();  // 기존 role 저장(USER 권한)

        //when
        userService.updateRole(me.getId(), Role.ADMIN);

        //then
        User updated = userRepository.findById(me.getId()).orElseThrow();
        assertThat(updated.getRole()).isEqualTo(Role.ADMIN);

        // Kafka 이벤트 발행 검증 - 정확한 파라미터 확인
        verify(notificationEventProducer, times(1))
                .send(
                        eq(me.getId()),
                        eq(NotificationType.ROLE_UPDATED),
                        eq(oldRole.name()),      // 이전 role
                        eq(Role.ADMIN.name())    // 새 role
                );
    }

    @Test
    @DisplayName("실패: 권한을 변경할 사용자의 존재하지 않는 ID")
    void updateRole_Fail() {
        //given
        Long nonExistentId = 999L;  // ← DB에 없는 ID

        //when & then
        assertThrows(UserException.class,
                () -> userService.updateRole(nonExistentId, Role.ADMIN));
    }

    @Test
    @DisplayName("locked 상태 변경 전후 확인")
    void updateLocked_Success() {
        //given
        User me = userRepository.save(new User("me", "me@test.com", "pw"));
        assertThat(me.getLocked()).isFalse();

        //when
        userService.updateLocked(me.getId(), true);

        //then
        User updated = userRepository.findById(me.getId()).orElseThrow();
        assertThat(updated.getLocked()).isTrue();
    }

    @Test
    @DisplayName("실패: 존재하지 않는 사용자 ID")
    void updateLocked_Fail() {
        //given
        Long nonExistentId = 999L;  // ← DB에 없는 ID

        //when & then
        assertThrows(UserException.class,
                () -> userService.updateLocked(nonExistentId, true));
    }

    @Test
    @DisplayName("성공: 사용자의 비밀번호를 변경한다")
    void updatePassword_Success() {
        //given
        User user = userRepository.save(new User("me", "me@test.com", "oldpw"));
        String newPassword = "newPassword123";

        //when
        userService.updatePassword(user.getId(), newPassword);

        //then
        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getPassword()).isNotEqualTo(newPassword);
        assertThat(passwordEncoder.matches(newPassword, updated.getPassword())).isTrue();
    }

    @Test
    @DisplayName("성공: 이름과 이미지를 모두 업데이트한다")
    void updateImage_NameAndImage() throws Exception {
        //given
        User user = userRepository.save(new User("oldName", "test@test.com", "pw"));
        Long userId = user.getId();

        // Mock 이미지 파일 생성
        MockMultipartFile mockImage = new MockMultipartFile(
                "image",                          // 파라미터 이름
                "profile.jpg",                    // 원본 파일명
                "image/jpeg",                     // Content-Type
                "test image content".getBytes()   // 파일 내용
        );

        // S3 Mock 동작 설정
        String newImageUrl = "https://s3.amazonaws.com/bucket/new-profile.jpg";
        when(s3Manager.upload(any(UploadFileRequest.class), eq(FileCategory.PROFILE_IMAGE)))
                .thenReturn(newImageUrl);

        String presignedUrl = "https://s3.amazonaws.com/bucket/new-profile.jpg?presigned=xyz";
        when(s3Manager.generatePresignedUrl(newImageUrl))
                .thenReturn(presignedUrl);

        //when
        UserDto result = userService.updateImage(userId, "newName", mockImage);

        //then
        // 1. DB 확인
        User updated = userRepository.findById(userId).orElseThrow();
        assertThat(updated.getName()).isEqualTo("newName");
        assertThat(updated.getProfileImageUrl()).isEqualTo(newImageUrl);

        // 2. S3 호출 확인
        verify(s3Manager).upload(any(UploadFileRequest.class), eq(FileCategory.PROFILE_IMAGE));
        verify(s3Manager).generatePresignedUrl(newImageUrl);

        // 3. 반환값 확인
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("newName");
        assertThat(result.profileImageUrl()).isEqualTo(presignedUrl);
    }

    @Test
    @DisplayName("실패: 빈 이미지 파일은 무시됨")
    void updateImage_EmptyImage() {
        //given
        User user = userRepository.save(new User("oldName", "test@test.com", "pw"));

        MockMultipartFile emptyImage = new MockMultipartFile(
                "image",
                "empty.jpg",
                "image/jpeg",
                new byte[0]  // 빈 파일
        );

        //when
        UserDto result = userService.updateImage(user.getId(), "newName", emptyImage);

        //then
        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("newName");  // 이름만 변경
        assertThat(updated.getProfileImageUrl()).isNull();  // 이미지 변경 안됨

        // S3 호출 안됨
        verify(s3Manager, never()).upload(any(), any());
    }

    @Test
    @DisplayName("성공: 검색 조건 없이 전체 사용자 조회(다음 페이지 있음)")
    void findAllUsers_NoCondition() {
        //given
        createTestUsers();  // 10명 생성

        UserSearchCondition condition = new UserSearchCondition(
                null,  // emailLike
                null,  // roleEqual
                null,  // isLocked
                null,  // cursor
                null,  // idAfter
                5,    // limit
                SortDirection.DESCENDING, //default
                SortBy.name //default
        );

        //when
        PageResponse<UserDto> result = userService.findAllUsers(condition);

        //then
        assertThat(result.data()).hasSize(5);
        assertThat(result.hasNext()).isTrue();  // limit(5) < 데이터(10)
        assertThat(result.nextCursor()).isNotNull();
        assertThat(result.nextIdAfter()).isNotNull();
        assertThat(result.totalCount()).isEqualTo(6);

        // ✅ 추가: 정렬 순서 확인
        assertThat(result.data().get(0).name()).isEqualTo("Jack");  // DESC: C > B > A
        assertThat(result.data().get(1).name()).isEqualTo("Iris");
        assertThat(result.data().get(2).name()).isEqualTo("Henry");
    }


}
