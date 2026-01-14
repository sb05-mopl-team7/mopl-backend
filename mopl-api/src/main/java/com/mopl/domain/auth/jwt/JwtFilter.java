package com.mopl.domain.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            // 헤더에서 액세스 토큰 추출
            String token = jwtTokenProvider.resolveToken(request);

            // 토큰 유효성 검증 및 인증 정보(Authentication) 설정
            if (token != null && jwtTokenProvider.validateAccessToken(token)) {
                Authentication authentication = jwtTokenProvider.getAuthentication(token);

                // SecurityContext에 인증 객체 저장 (요청 처리 동안 유지)
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("JWT 인증 성공: {}", authentication.getName());
            }

            // 다음 필터로 요청 전달
            filterChain.doFilter(request, response);

        } catch (Exception e) {
            // 응답이 이미 클라이언트로 전송 시작했는지 확인
            if (response.isCommitted()) {
                // 이미 커밋된 상태에서 다시 예외를 처리하려고 하면 'Response Committed' 에러가 발생
                log.warn("응답이 이미 커밋되어 예외를 처리할 수 없습니다: {}", e.getMessage());
                return;
            }

            // 발생한 예외를 Request 속성에 저장하여 EntryPoint 등에서 활용할 수 있게 함
            request.setAttribute("exception", e);

            // 예외 발생 시에도 필터 체인을 계속 진행하여 AuthenticationEntryPoint에서 처리하도록 유도
            filterChain.doFilter(request, response);
        }
    }
}
