package com.mopl.domain.user.service;

import com.mopl.domain.notification.enums.NotificationType;
import com.mopl.domain.notification.producer.NotificationEventProducer;
import com.mopl.domain.user.dto.UserCreateRequest;
import com.mopl.domain.user.dto.UserDto;
import com.mopl.domain.user.dto.UserSearchCondition;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.enums.Role;
import com.mopl.domain.user.exception.UserErrorCode;
import com.mopl.domain.user.exception.UserException;
import com.mopl.domain.user.mapper.UserMapper;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.global.dto.PageResponse;
import com.mopl.global.dto.UploadFileRequest;
import com.mopl.global.enums.SortBy;
import com.mopl.global.enums.SortDirection;
import com.mopl.global.s3.FileCategory;
import com.mopl.global.s3.S3Manager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final S3Manager s3Manager;
    private final NotificationEventProducer notificationEventProducer;

    @Transactional(readOnly = true)
    public Boolean existUser(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional
    public UserDto createUser(UserCreateRequest dto) {
        if (existUser(dto.email())) {
            throw new UserException(UserErrorCode.DUPLICATE_USER);
        }
        User user = new User(dto.name(), dto.email(), passwordEncoder.encode(dto.password()));
        User createdUser = userRepository.save(user);
        return userMapper.toDto(createdUser);
    }

    @PreAuthorize("principal.userId == #userId")
    @Transactional
    public UserDto updateImage(long userId, String name, MultipartFile image) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_EXIST));
        if (name != null && !name.isBlank()) user.updateName(name);

        if (image != null && !image.isEmpty()) {
            try {
                //기존 이미지 백업
                String oldImageUrl = user.getProfileImageUrl();
                //새 이미지 저장
                UploadFileRequest profileImage = toUploadFileRequest(image);
                String newAvatar = s3Manager.upload(profileImage, FileCategory.PROFILE_IMAGE);
                //db 업데이트
                user.updateProfileImageUrl(newAvatar);
                //기존 이미지 삭제
                if (user.getProfileImageUrl() != null) {
                    s3Manager.delete(oldImageUrl);
                }
            } catch (IOException e) {
                throw new RuntimeException("프로필 이미지 업로드 실패", e);
            }
        }
        String thumbnailUrl = s3Manager.generatePresignedUrl(user.getProfileImageUrl());
        return userMapper.toDto(user, thumbnailUrl);
    }

    private UploadFileRequest toUploadFileRequest(MultipartFile image) throws IOException {
        return new UploadFileRequest(
                image.getInputStream(),
                image.getOriginalFilename(),
                image.getSize(),
                image.getContentType()
        );
    }

    @Transactional
    public void updateRole(Long userId, Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_EXIST));

        Role role = user.getRole();
        if (user.updateRole(newRole)) {
            notificationEventProducer.send(userId, NotificationType.ROLE_UPDATED, role.name(), newRole.name());
        }
    }

    @Transactional
    public void updateLocked(Long userId, boolean newLocked) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_EXIST));
        user.updateLocked(newLocked);
    }

    @Transactional
    public void updatePassword(Long userId, String Password) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_EXIST));
        String newPassword = passwordEncoder.encode(Password);
        user.updatePassword(newPassword);

    }

    @Transactional(readOnly = true)
    public UserDto detail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.USER_NOT_EXIST));
        String thumbnailUrl = s3Manager.generatePresignedUrl(user.getProfileImageUrl());
        return userMapper.toDto(user, thumbnailUrl);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserDto> findAllUsers(UserSearchCondition condition) {
        UserSearchCondition.StartId startId = condition.parseStartId();

        Sort sort = buildSort(condition.sortBy(), condition.sortDirection());
        Pageable pageable = PageRequest.of(0, condition.limit() + 1, sort);

        List<User> fetched = userRepository.cursorFindAll(
                condition.emailLike(),
                condition.roleEqual(),
                condition.isLocked(),
                startId.sortByProperty(),
                startId.cursorValue(),
                startId.idAfter(),
                pageable
        );

        boolean hasNext = fetched.size() > condition.limit();
        List<User> page = hasNext
                ? fetched.subList(0, condition.limit())
                : fetched;

        List<UserDto> data = userMapper.toDtoList(page);

        Long totalCount = (long) fetched.size();

        String nextCursor = null;
        String nextIdAfter = null;
        if (hasNext && !page.isEmpty()) {
            User last = page.get(page.size() - 1);
            nextCursor = condition.formatCursor(last);
            nextIdAfter = String.valueOf(last.getId());
        }

        return PageResponse.<UserDto>builder()
                .data(data != null ? data : List.of())
                .nextCursor(nextCursor)
                .nextIdAfter(nextIdAfter)
                .hasNext(hasNext)
                .totalCount(totalCount)
                .sortBy(condition.sortBy().toString())
                .sortDirection(condition.sortDirection())
                .build();
    }

    /**
     * 정렬 조건 생성
     * 1차: sortBy (사용자 선택)
     * 2차: createdAt (sortBy가 createdAt이 아닐 때)
     * 3차: id (tie-breaker)
     */
    private Sort buildSort(SortBy sortBy, SortDirection sortDirection) {
        Sort.Direction direction = sortDirection.toSpring();

        // 1차 정렬: 사용자가 선택한 기준
        Sort sort = Sort.by(direction, sortBy.property());

        // 2차 정렬: createdAt (sortBy가 createdAt가 아닐 때만)
        if (sortBy != SortBy.createdAt) {
            sort = sort.and(Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        // 3차 정렬: id (항상 추가, tie-breaker)
        sort = sort.and(Sort.by(Sort.Direction.DESC, "id"));

        return sort;
    }
}
