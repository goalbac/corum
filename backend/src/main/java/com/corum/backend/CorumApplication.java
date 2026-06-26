package com.corum.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
@org.springframework.scheduling.annotation.EnableScheduling
public class CorumApplication {

    public static void main(String[] args) {
        SpringApplication.run(CorumApplication.class, args);
    }
}
