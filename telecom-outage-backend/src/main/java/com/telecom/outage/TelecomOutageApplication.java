package com.telecom.outage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TelecomOutageApplication {

    public static void main(String[] args) {
        SpringApplication.run(
                TelecomOutageApplication.class,
                args
        );
    }
}