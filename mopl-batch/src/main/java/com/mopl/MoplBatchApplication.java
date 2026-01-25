package com.mopl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MoplBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoplBatchApplication.class, args);
    }

}