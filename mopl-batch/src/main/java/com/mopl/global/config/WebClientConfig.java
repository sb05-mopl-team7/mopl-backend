package com.mopl.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${tmdb.base-url}")
    private String tmdbBaseUrl;

    @Value("${tmdb.token}")
    private String tmdbToken;

//    @Value("${sportdb.base-url}")
//    private String sportDbBaseUrl;

    @Bean(name = "tmdbWebClient")
    public WebClient tmdbWebClient() {
        return WebClient.builder()
                .baseUrl(tmdbBaseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + tmdbToken)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

//    @Bean(name = "sportDbWebClient")
//    public WebClient sportDbWebClient() {
//        return WebClient.builder()
//                .baseUrl(sportDbBaseUrl)
//                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
//                .build();
//    }
}
