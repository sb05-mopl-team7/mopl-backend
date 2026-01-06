package com.mopl.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mopl.domain.auth.dto.JwtDto;
import com.mopl.domain.auth.dto.JwtInformation;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.ErrorResponse;
import com.nimbusds.jose.JOSEException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final ObjectMapper objectMapper;
    private final JwtTokenProvider tokenProvider;
    private final JwtRegistry jwtRegistry;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        if(authentication.getPrincipal() instanceof MoplUserDetails userDetails){
            try{
                String accessToken = tokenProvider.generateAccessToken(userDetails);
                String refreshToken = tokenProvider.generateRefreshToken(userDetails);

                Cookie refreshTokenCookie = tokenProvider.genereateRefreshTokenCookie(refreshToken);
                response.addCookie(refreshTokenCookie);

                JwtDto jwtDto = new JwtDto(userDetails.getUserDto(), accessToken);

                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(objectMapper.writeValueAsString(jwtDto));

                jwtRegistry.registerJwtInformation(
                        new JwtInformation(
                                userDetails.getUserDto(),
                                accessToken,
                                refreshToken
                        )
                );
            }catch (JOSEException e) {
                log.error("Failed to generate JWT token for user: {}", userDetails.getUsername(), e);
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                ErrorResponse errorResponse = new ErrorResponse(
                        new RuntimeException("Token generation failed"),
                        ErrorCode.INTERNAL_SERVER_ERROR
                );
                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            }
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            ErrorResponse errorResponse = new ErrorResponse(
                    new RuntimeException("Authentication failed: Invalid user details"),
                    ErrorCode.UNAUTHORIZED
            );
            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        }
    }
}
