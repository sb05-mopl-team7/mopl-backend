package com.mopl.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SseConfig implements WebMvcConfigurer {

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        // 프론트엔드 재연결 주기에 맞춰 60초로 설정해 안정적인 연결을 유지
        configurer.setDefaultTimeout(60 * 1000L);
    }

}