package com.mopl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MoplApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoplApiApplication.class, args);
    }

}