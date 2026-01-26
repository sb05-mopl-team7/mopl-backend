package com.mopl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MoplBatchApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(MoplBatchApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE); // 웹 서버 미기동 설정
        app.run(args);
    }

}