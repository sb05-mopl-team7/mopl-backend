package com.mopl.domain.auth.jwt;

import com.mopl.global.exception.ErrorCode;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
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
            if (token != null) {
                jwtTokenProvider.validateAccessToken(token);

                Authentication authentication = jwtTokenProvider.getAuthentication(token);

                // SecurityContext에 인증 객체 저장 (요청 처리 동안 유지)
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("JWT 인증 성공: {}", authentication.getName());
            }
        } catch (ExpiredJwtException e) {
            request.setAttribute("exception", ErrorCode.EXPIRED_TOKEN);
        } catch (JwtException | IllegalArgumentException e) {
            // 위조되거나 잘못된 토큰
            request.setAttribute("exception", ErrorCode.INVALID_TOKEN);
        } catch (Exception e) {
            log.error("JWT 필터 내부 오류", e);
            // 발생한 예외를 Request 속성에 저장하여 EntryPoint 등에서 활용할 수 있게 함
            request.setAttribute("exception", ErrorCode.INVALID_TOKEN);
        }

        // 예외 발생 시에도 필터 체인을 계속 진행하여 AuthenticationEntryPoint에서 처리하도록 유도
        filterChain.doFilter(request, response);
    }
}
