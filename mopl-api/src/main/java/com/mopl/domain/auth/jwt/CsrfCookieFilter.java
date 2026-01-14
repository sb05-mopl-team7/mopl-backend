package com.mopl.domain.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());

        if (csrfToken != null) {
            // getToken() 호출로 실제 토큰 생성 트리거
            String tokenValue = csrfToken.getToken();

            // 요청 쿠키에서 현재 CSRF 토큰 확인
            String existingToken = null;
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if ("XSRF-TOKEN".equals(cookie.getName())) {
                        existingToken = cookie.getValue();
                        break;
                    }
                }
            }

            // 👇 핵심: 쿠키가 없거나 값이 다를 때만 새로 설정
            if (existingToken == null || !existingToken.equals(tokenValue)) {
                Cookie cookie = new Cookie("XSRF-TOKEN", tokenValue);
                cookie.setPath("/");
                cookie.setHttpOnly(false);
                cookie.setSecure(false);
                cookie.setMaxAge(3600);  // 👈 1시간 유지 (세션 쿠키 대신)

                response.addCookie(cookie);
                log.debug("CSRF 토큰 쿠키 업데이트: {}", tokenValue);
            } else {
                log.debug("CSRF 토큰 쿠키 유지: {}", existingToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}