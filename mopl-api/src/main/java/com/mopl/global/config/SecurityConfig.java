package com.mopl.global.config;

import com.mopl.domain.auth.jwt.JwtFilter;
import com.mopl.domain.auth.jwt.JwtTokenProvider;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                                .requestMatchers(
                                        "/api-docs/**",
                                        "/swagger-ui/**",
                                        "/swagger.html",
                                        "/favicon.ico",
                                        "/index.html",
                                        "/actuator/**",
                                        "/h2-console/**"
                                ).permitAll()

                                .requestMatchers(
                                        "/api/auth/**",
                                        "/api/users/"
                                ).permitAll()
                                .anyRequest().authenticated()
                )
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                // 세션 기반 인증 기능 비활성화
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                // JWT 필터
                .addFilterBefore(new JwtFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class)
                // 2) CSRF 설정 (Cookie 방식)
                .csrf(csrf -> csrf.disable()) // TODO : 나중에 변경예정
//                .csrf(csrf -> csrf
//                        .ignoringRequestMatchers("/api/auth/logout")
//                        .ignoringRequestMatchers("/api/sse/**")
//                        .ignoringRequestMatchers("/api/users/**")
//                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
//                )
                // 인증 실패
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                );

        return http.build();
    }

    /**
     * 비밀번호 암호화
     */
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 인증되지 않은 사용자가 보호된 리소스에 접근할 때 호출 (예: 로그인 하지 않은 상태에서 API 호출)
     */
    private AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            throw new MoplException(ErrorCode.FORBIDDEN);
        };
    }
}
