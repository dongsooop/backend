package com.dongsoop.dongsoop.notification.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationConfig {

    @Bean
    public ExecutorService notificationExecutor() {
        return Executors.newFixedThreadPool(5);
    }
}
