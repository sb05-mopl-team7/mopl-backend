package com.mopl.domain.review.controller;

import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc; // ✅ 요청대로 그대로 유지
import org.springframework.test.context.ActiveProfiles;

import tools.jackson.databind.ObjectMapper;
import com.mopl.domain.auth.dto.UserPrincipal;
import com.mopl.domain.content.entity.Content;
import com.mopl.domain.content.enums.ContentType;
import com.mopl.domain.content.repository.ContentRepository;
import com.mopl.domain.review.dto.request.ReviewCreateRequest;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.global.exception.ErrorCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReviewApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private ContentRepository contentRepository;
    @Autowired private ObjectMapper objectMapper;

    private User user1;
    private User user2;
    private Content content;

    @BeforeEach
    void setUp() {
        user1 = userRepository.save(new User("me", "user1@test.com", "password"));
        user2 = userRepository.save(new User("you", "user2@test.com", "password"));

        content = contentRepository.save(createTestContent());
    }

    // 인증 통과만 시키기

    private RequestPostProcessor loginAs(User user) {
        UserPrincipal principal = createPrincipal(user);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        return authentication(auth);
    }

    //Mockito 없이 테스트용 UserPrincipal 생성

    private UserPrincipal createPrincipal(User user) {
        try {
            // 1) static factory: of/from/create(User) 또는 of/from/create(Long|long)
            for (String name : List.of("of", "from", "create")) {
                UserPrincipal p1 = tryStaticFactory(name, User.class, user);
                if (p1 != null) return p1;

                UserPrincipal p2 = tryStaticFactory(name, Long.class, user.getId());
                if (p2 != null) return p2;

                UserPrincipal p3 = tryStaticFactory(name, long.class, user.getId());
                if (p3 != null) return p3;
            }

            // 2) 생성자 자동 매칭: 파라미터 수 적은 것부터 시도
            Constructor<?>[] ctors = UserPrincipal.class.getDeclaredConstructors();
            Arrays.sort(ctors, Comparator.comparingInt(Constructor::getParameterCount));

            for (Constructor<?> ctor : ctors) {
                ctor.setAccessible(true);

                Class<?>[] paramTypes = ctor.getParameterTypes();
                Object[] args = new Object[paramTypes.length];

                for (int i = 0; i < paramTypes.length; i++) {
                    args[i] = defaultArgFor(paramTypes[i], user);
                }

                try {
                    return (UserPrincipal) ctor.newInstance(args);
                } catch (Exception ignored) {
                }
            }

            throw new IllegalStateException("UserPrincipal 생성 실패: 사용 가능한 팩토리/생성자를 찾지 못함");

        } catch (Exception e) {
            throw new IllegalStateException("테스트용 UserPrincipal 생성 중 오류: " + e.getMessage(), e);
        }
    }

    private UserPrincipal tryStaticFactory(String methodName, Class<?> paramType, Object arg) {
        try {
            Method m = UserPrincipal.class.getDeclaredMethod(methodName, paramType);
            if (!Modifier.isStatic(m.getModifiers())) return null;
            m.setAccessible(true);
            return (UserPrincipal) m.invoke(null, arg);
        } catch (NoSuchMethodException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    //생성자 파라미터 타입에 따라 넣을 기본값 생성
    private Object defaultArgFor(Class<?> type, User user) {
        if (type == Long.class || type == long.class) return user.getId();

        if (type == String.class) return user.getEmail();

        if (type == User.class) return user;

        if (type == boolean.class || type == Boolean.class) return true;
        if (type == int.class || type == Integer.class) return 0;
        if (type == double.class || type == Double.class) return 0.0;

        if (type == SimpleGrantedAuthority.class) return new SimpleGrantedAuthority("ROLE_USER");
        if (type == GrantedAuthority.class) return new SimpleGrantedAuthority("ROLE_USER");

        // UserDetails가 필요한 경우 대비
        if (type.getName().equals("org.springframework.security.core.userdetails.UserDetails")) {
            return new org.springframework.security.core.userdetails.User(
                    String.valueOf(user.getId()),
                    "N/A",
                    List.of(new SimpleGrantedAuthority("ROLE_USER"))
            );
        }

        // authorities 같은 경우
        if (Collection.class.isAssignableFrom(type)) return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        if (Set.class.isAssignableFrom(type)) return Set.of(new SimpleGrantedAuthority("ROLE_USER"));
        if (Map.class.isAssignableFrom(type)) return Map.of();

        if (type.isEnum()) return type.getEnumConstants()[0];

        return null;
    }

    // Content 엔티티 생성자에 맞춘 최소 세팅
    private Content createTestContent() {
        return new Content(
                ContentType.values()[0], // enum 상수 이름 몰라도 안전하게 1개 선택
                "테스트 콘텐츠 제목",
                "테스트 콘텐츠 설명",
                "https://example.com/thumb.png"
        );
    }

    // 1) 성공 (201)
    @Test
    @DisplayName("[201] 정상적인 리뷰 생성 요청 시 성공한다")
    void createReview_Success() throws Exception {
        // Given
        ReviewCreateRequest request = new ReviewCreateRequest(
                content.getId(),
                "재밌게 봤어요!",
                5.0
        );

        // When & Then
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(loginAs(user1)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentId").value(content.getId()))
                .andExpect(jsonPath("$.text").value("재밌게 봤어요!"))
                .andExpect(jsonPath("$.rating").value(5.0));
    }

    // 2) 비즈니스 예외 (400) - 최신 정렬만 허용(createdAt + DESCENDING)
    @Test
    @DisplayName("[400] 최신 정렬이 아니면 INVALID_REQUEST 에러 발생")
    void findAll_InvalidSort_Fail() throws Exception {
        mockMvc.perform(get("/api/reviews")
                        .param("contentId", String.valueOf(content.getId()))
                        .param("sortBy", "rating")              // ❌ createdAt만 허용
                        .param("sortDirection", "ASCENDING")    // ❌ DESCENDING만 허용
                        .with(loginAs(user1)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value(ErrorCode.INVALID_REQUEST.name()))
                .andExpect(jsonPath("$.detail").value(ErrorCode.INVALID_REQUEST.getMessage()));
    }

    // 3) 인증 실패 (401)
    @Test
    @DisplayName("[401] 로그인 정보 없이 요청하면 UNAUTHORIZED 에러 발생")
    void createReview_Unauthorized_Fail() throws Exception {
        ReviewCreateRequest request = new ReviewCreateRequest(
                content.getId(),
                "내용",
                4.0
        );

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value(ErrorCode.UNAUTHORIZED.name()));
    }

    // 4) 리소스 없음 (404) - 존재하지 않는 contentId
    @Test
    @DisplayName("[404] 존재하지 않는 콘텐츠에 리뷰 생성하면 NOT_FOUND 에러 발생")
    void createReview_ContentNotFound_Fail() throws Exception {
        Long nonExistentContentId = 999999L;

        ReviewCreateRequest request = new ReviewCreateRequest(
                nonExistentContentId,
                "내용",
                3.0
        );

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(loginAs(user1)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value(ErrorCode.NOT_FOUND.name()));
    }
}