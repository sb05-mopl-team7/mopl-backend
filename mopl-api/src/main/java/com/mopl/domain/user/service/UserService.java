package com.mopl.domain.user.service;

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
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
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
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final S3Manager s3Manager;

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
    public UserDto updateImage(long userId, String name, MultipartFile image){
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
        user.updateRole(newRole);
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

    public PageResponse<UserDto> findAllUsers(UserSearchCondition searchCondition) {
        String keywordLike = searchCondition.emailLike();
        Role roleEqual = searchCondition.roleEqual();
        Boolean isLocked = searchCondition.isLocked();
        String cursor = searchCondition.cursor();
        String idAfter = searchCondition.idAfter();
        int limit = searchCondition.limit();
        SortDirection sortDirection = searchCondition.sortDirection();
        SortBy sortBy = searchCondition.sortBy();

        StartId key = parseStartId(sortBy,cursor, idAfter);
        Sort sort = buildSort(sortBy, sortDirection);
        Pageable pageable = PageRequest.of(0, limit + 1, sort);

        List<User> fetched = userRepository.cursorFindAll(
                keywordLike,
                roleEqual,
                isLocked,
                key.sortByProperty,
                key.cursorValue,
                key.idAfter,
                pageable
        );

        Long totalCount = (long) fetched.size();

        boolean hasNext = fetched.size() > limit;

        List<User> page = hasNext
                ? fetched.subList(0, limit)
                : fetched;

        List<UserDto> data = page.stream()
                .map(this::toDto)
                .toList();

        String nextCursor = null;
        String nextIdAfter = null;

        if (hasNext && !page.isEmpty()) {
            User last = page.get(page.size() - 1);
            nextIdAfter = String.valueOf(last.getId());
            nextCursor = formatCursorValue(sortBy, last);
        }

        return PageResponse.<UserDto>builder()
                .data(data != null ? data : List.of())
                .nextCursor(nextCursor)
                .nextIdAfter(nextIdAfter)
                .hasNext(hasNext)
                .totalCount(totalCount)
                .sortBy(sortBy.toString())
                .sortDirection(sortDirection)
                .build();
    }
    private String formatCursorValue(SortBy sortBy, User user) {
        switch (sortBy) {
            case name:
                return user.getName();

            case email:
                return user.getEmail();

            case createdAt:
                return user.getCreatedAt()
                        .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            case role:
                return user.getRole().name();

            case isLocked:
                return String.valueOf(user.getLocked());

            default:
                throw new MoplException(ErrorCode.INVALID_REQUEST);
        }
    }


    private StartId parseStartId(
            SortBy sortBy,
            String cursorRaw,
            String idAfterRaw) {
        boolean hasCursor = cursorRaw != null && !cursorRaw.isBlank();
        boolean hasIdAfter = idAfterRaw != null && !idAfterRaw.isBlank();

        if (hasCursor != hasIdAfter) {
            throw new MoplException(ErrorCode.INVALID_REQUEST);
        }

        if (!hasCursor) {
            return new StartId(sortBy.property(),null, null);
        }
        // sortBy에 따라 cursor 파싱
        Object parsedCursor = parseCursorValue(sortBy, cursorRaw);
        Long parsedId = parseLong(idAfterRaw);

        return new StartId(sortBy.property(),parsedCursor,parsedId);
    }
    private Object parseCursorValue(SortBy sortBy, String cursorRaw) {
        try {
            switch (sortBy) {
                case name:
                case email:
                    return cursorRaw;  // String 그대로

                case createdAt:
                    return parseCreatedAtCursor(cursorRaw);  // LocalDateTime

                case role:
                    // Role enum 파싱
                    return Role.valueOf(cursorRaw.toUpperCase());

                case isLocked:
                    // Boolean 파싱
                    return Boolean.parseBoolean(cursorRaw);

                default:
                    throw new MoplException(ErrorCode.INVALID_REQUEST);
            }
        } catch (Exception e) {
            throw new MoplException(ErrorCode.INVALID_REQUEST);
        }
    }


    private LocalDateTime parseCreatedAtCursor(String cursor) {
        String normalized = cursor.trim().replace(" ", "T");
        try {
            return LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeException e) {
            throw new MoplException(ErrorCode.INVALID_REQUEST);
        }
    }

    private Long parseLong(String idAfter) {
        try {
            return Long.parseLong(idAfter.trim());
        } catch (Exception e) {
            throw new MoplException(ErrorCode.INVALID_REQUEST);
        }
    }

    // Sort 구성 메서드 추가
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

    private String formatCreatedAtCursor(LocalDateTime createdAt) {
        return createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    private UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getCreatedAt(),
                user.getEmail(),
                user.getName(),
                user.getProfileImageUrl(),
                user.getRole(),
                user.getLocked()
        );
    }

    private record StartId(String sortByProperty,Object cursorValue, Long idAfter) {

    }
    @Transactional(readOnly = true)
    public UserDto detail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(()-> new UserException(UserErrorCode.USER_NOT_EXIST));
        String thumbnailUrl = s3Manager.generatePresignedUrl(user.getProfileImageUrl());
        return userMapper.toDto(user, thumbnailUrl);
    }



}
