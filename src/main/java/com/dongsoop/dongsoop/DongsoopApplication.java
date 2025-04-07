package com.dongsoop.dongsoop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DongsoopApplication {

    public static void main(String[] args) {
        SpringApplication.run(DongsoopApplication.class, args);
    }

}
