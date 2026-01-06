package com.mopl.global.config;

import com.mopl.domain.user.enums.Role;
import com.mopl.security.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    //비밀번호 암호화
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder(){
        return new BCryptPasswordEncoder();
    };

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtLoginSuccessHandler jwtLoginSuccessHandler,
            LoginFailureHandler loginFailureHandler
                                                   ) throws Exception{

        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(login->login
                        .loginProcessingUrl("/api/auth/sign-in")
                        .successHandler(jwtLoginSuccessHandler)
                        .failureHandler(loginFailureHandler))
                .httpBasic(AbstractHttpConfigurer::disable)
                //인가
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                // Swagger UI
                                "/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger.html"
                        ).permitAll()
                        //테스트 용
                        .requestMatchers("/api/users/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .anyRequest().hasRole(Role.USER.name())
                )
                //예외처리
                .exceptionHandling(e-> e
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                        })
                        .accessDeniedHandler((request, response, authException) -> {
                            response.sendError(HttpServletResponse.SC_FORBIDDEN); // 403 응답
                        }))
                //세션 처리
                .sessionManagement(session->session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
    @Bean
    public JwtRegistry<Long> jwtRegistry(
            JwtTokenProvider jwtTokenProvider,
            ApplicationEventPublisher eventPublisher
    ) {
        return new InMemoryJwtRegistry<>(1, jwtTokenProvider, eventPublisher);
    }
}
