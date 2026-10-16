package com.skillcheckr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SkillCheckrApplication {

    public static void main(String[] args) {
        SpringApplication.run(SkillCheckrApplication.class, args);
    }

}