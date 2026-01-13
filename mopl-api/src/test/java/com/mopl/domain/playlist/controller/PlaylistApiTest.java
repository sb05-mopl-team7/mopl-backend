package com.mopl.domain.playlist.controller;

import com.jayway.jsonpath.JsonPath;
import com.mopl.domain.auth.dto.UserPrincipal;
import com.mopl.domain.user.entity.User;
import com.mopl.domain.user.repository.UserRepository;
import com.mopl.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "jwt.access-secret=test-access-secret-test-access-secret-test-access-secret",
        "jwt.refresh-secret=test-refresh-secret-test-refresh-secret-test-refresh-secret",

        "spring.mail.username=test@test.com",
        "spring.mail.password=test-password",
        "spring.mail.host=localhost",
        "spring.mail.port=25"
})
class PlaylistApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;

    private User owner;
    private User subscriber;

    @BeforeEach
    void setUp() {
        owner = userRepository.save(new User("owner", "owner@test.com", "password"));
        subscriber = userRepository.save(new User("sub", "sub@test.com", "password"));
    }

    private RequestPostProcessor login(User user, String role) {
        UserPrincipal principal = buildUserPrincipal(user, role);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        return authentication(auth);
    }

    private UserPrincipal buildUserPrincipal(User user, String role) {
        try {
            for (String methodName : List.of("of", "from", "create")) {
                Optional<UserPrincipal> byFactory = tryStaticFactory(methodName, user, role);
                if (byFactory.isPresent()) return byFactory.get();
            }

            List<Constructor<?>> ctors = new ArrayList<>(List.of(UserPrincipal.class.getDeclaredConstructors()));
            ctors.sort((a, b) -> Integer.compare(b.getParameterCount(), a.getParameterCount()));

            for (Constructor<?> ctor : ctors) {
                ctor.setAccessible(true);
                Object[] args = buildArgs(ctor.getParameterTypes(), user, role);
                try {
                    return (UserPrincipal) ctor.newInstance(args);
                } catch (Exception ignore) {}
            }

            throw new IllegalStateException("UserPrincipal 생성 실패: 프로젝트 UserPrincipal 생성자/팩토리를 확인하세요.");
        } catch (Exception e) {
            throw new IllegalStateException("UserPrincipal 생성 실패: " + e.getMessage(), e);
        }
    }

    private Optional<UserPrincipal> tryStaticFactory(String methodName, User user, String role) {
        for (Method m : UserPrincipal.class.getDeclaredMethods()) {
            if (!m.getName().equals(methodName)) continue;
            if (!java.lang.reflect.Modifier.isStatic(m.getModifiers())) continue;
            if (!UserPrincipal.class.isAssignableFrom(m.getReturnType())) continue;

            try {
                m.setAccessible(true);
                Object[] args = buildArgs(m.getParameterTypes(), user, role);
                return Optional.of((UserPrincipal) m.invoke(null, args));
            } catch (Exception ignore) {}
        }
        return Optional.empty();
    }

    private Object[] buildArgs(Class<?>[] paramTypes, User user, String role) {
        Object[] args = new Object[paramTypes.length];

        Deque<String> stringPool = new ArrayDeque<>();
        stringPool.add(user.getEmail());
        stringPool.add(role);
        stringPool.add(user.getName());
        stringPool.add("N/A");

        for (int i = 0; i < paramTypes.length; i++) {
            Class<?> t = paramTypes[i];

            if (t == Long.class || t == long.class) {
                args[i] = user.getId();
                continue;
            }
            if (t == String.class) {
                args[i] = stringPool.isEmpty() ? "N/A" : stringPool.removeFirst();
                continue;
            }
            if (t.isEnum()) {
                args[i] = pickEnumValue(t, role);
                continue;
            }
            if (Collection.class.isAssignableFrom(t) || List.class.isAssignableFrom(t) || Set.class.isAssignableFrom(t)) {
                args[i] = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                continue;
            }
            if (t == boolean.class || t == Boolean.class) {
                args[i] = true;
                continue;
            }
            args[i] = null;
        }
        return args;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private Object pickEnumValue(Class<?> enumType, String role) {
        Object[] constants = enumType.getEnumConstants();
        if (constants == null || constants.length == 0) return null;

        for (Object c : constants) {
            String name = ((Enum) c).name();
            if (name.equalsIgnoreCase(role) || name.equalsIgnoreCase("ROLE_" + role)) return c;
        }
        return constants[0];
    }

    private Long createPlaylistAs(User who) throws Exception {
        String body = """
                {
                  "title": "테스트 플레이리스트",
                  "description": "설명"
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/playlists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(login(who, "USER")))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        Number id = JsonPath.read(json, "$.id");
        return id.longValue();
    }

    // 1) 생성 성공
    @Test
    @DisplayName("[2xx] 플레이리스트 생성 성공")
    void createPlaylist_Success() throws Exception {
        String body = """
                {
                  "title": "내 플레이리스트",
                  "description": "테스트용"
                }
                """;

        mockMvc.perform(post("/api/playlists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(login(owner, "USER")))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.title").value("내 플레이리스트"))
                .andExpect(jsonPath("$.description").value("테스트용"));
    }

    // 2) 인증 실패(생성)
    @Test
    @DisplayName("[401] 로그인 없이 생성 요청하면 UNAUTHORIZED")
    void createPlaylist_Unauthorized_Fail() throws Exception {
        String body = """
                {
                  "title": "내 플레이리스트",
                  "description": "테스트용"
                }
                """;

        mockMvc.perform(post("/api/playlists")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value(ErrorCode.UNAUTHORIZED.name()));
    }

    // 3) 단건 조회 성공
    @Test
    @DisplayName("[200] 플레이리스트 단건 조회 성공")
    void findPlaylist_Success() throws Exception {
        Long playlistId = createPlaylistAs(owner);

        mockMvc.perform(get("/api/playlists/{playlistId}", playlistId)
                        .with(login(owner, "USER")))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(playlistId.intValue()));
    }

    // 4) 구독 성공
    @Test
    @DisplayName("[204] 다른 사용자의 플레이리스트를 구독하면 성공")
    void subscribe_Success() throws Exception {
        Long playlistId = createPlaylistAs(owner);

        mockMvc.perform(post("/api/playlists/{playlistId}/subscription", playlistId)
                        .with(login(subscriber, "USER")))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    // 5) 구독 취소 성공
    @Test
    @DisplayName("[204] 구독 취소 성공")
    void unsubscribe_Success() throws Exception {
        Long playlistId = createPlaylistAs(owner);

        mockMvc.perform(post("/api/playlists/{playlistId}/subscription", playlistId)
                        .with(login(subscriber, "USER")))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/playlists/{playlistId}/subscription", playlistId)
                        .with(login(subscriber, "USER")))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    // 6) 수정 성공
    @Test
    @DisplayName("[200] 플레이리스트 수정 성공")
    void updatePlaylist_Success() throws Exception {
        Long playlistId = createPlaylistAs(owner);

        String body = """
                {
                  "title": "수정된 제목",
                  "description": "수정된 설명"
                }
                """;

        mockMvc.perform(patch("/api/playlists/{playlistId}", playlistId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)
                        .with(login(owner, "USER")))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정된 제목"))
                .andExpect(jsonPath("$.description").value("수정된 설명"));
    }

    // 7) 삭제 성공
    @Test
    @DisplayName("[204] 소유자가 플레이리스트를 삭제하면 성공")
    void deletePlaylist_Success() throws Exception {
        Long playlistId = createPlaylistAs(owner);

        mockMvc.perform(delete("/api/playlists/{playlistId}", playlistId)
                        .with(login(owner, "USER")))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    // 8) 삭제 권한 실패
    @Test
    @DisplayName("[403] 소유자가 아닌 사용자가 삭제하면 FORBIDDEN")
    void deletePlaylist_Forbidden_Fail() throws Exception {
        Long playlistId = createPlaylistAs(owner);

        mockMvc.perform(delete("/api/playlists/{playlistId}", playlistId)
                        .with(login(subscriber, "USER")))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    // 9) 목록 조회 성공
    @Test
    @DisplayName("[200] 플레이리스트 목록 조회 성공 (커서 페이지네이션)")
    void findAllPlaylist_Success() throws Exception {
        createPlaylistAs(owner);
        createPlaylistAs(owner);

        mockMvc.perform(get("/api/playlists")
                        .param("limit", "10")
                        .param("sortBy", "updatedAt")
                        .param("sortDirection", "DESCENDING")
                        .with(login(owner, "USER")))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}
