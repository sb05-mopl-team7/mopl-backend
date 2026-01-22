package com.mopl.domain.auth.exception;

import com.mopl.global.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        // 응답이 이미 클라이언트로 전송 시작했는지 확인
        if (response.isCommitted()) {
            // 이미 커밋된 상태에서 다시 예외를 처리하려고 하면 'Response Committed' 에러가 발생
            log.warn("응답이 이미 커밋되어 예외를 처리할 수 없습니다.");
            return;
        }

        ErrorCode errorCode = (ErrorCode) request.getAttribute("exception"); // JwtFilter에서 담아둔 예외 확인
        if (errorCode == null) {
            errorCode = ErrorCode.UNAUTHORIZED;
        }

        log.warn("인증 실패: {} - {}", errorCode.getStatus(), errorCode.getMessage());

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatusCode.valueOf(errorCode.getStatus()),
                errorCode.getMessage()
        );

        pd.setTitle(errorCode.name());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setStatus(errorCode.getStatus());

        String json = objectMapper.writeValueAsString(pd);
        response.getWriter().write(json);
    }
}
