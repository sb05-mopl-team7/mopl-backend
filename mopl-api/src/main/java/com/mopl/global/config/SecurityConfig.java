package com.mopl.global.config;

import com.mopl.domain.user.enums.Role;
import jakarta.servlet.http.HttpServletResponse;
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
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception{

        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);
        //인가
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                // Swagger UI
                                "/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger.html"
                        ).permitAll()
                        .anyRequest().hasRole(Role.USER.name())
                );
        //예외처리
        http
                .exceptionHandling(e-> e
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                        })
                        .accessDeniedHandler((request, response, authException) -> {
                            response.sendError(HttpServletResponse.SC_FORBIDDEN); // 403 응답
                        }));
        //세션 처리
        http
                .sessionManagement(session->session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
}
