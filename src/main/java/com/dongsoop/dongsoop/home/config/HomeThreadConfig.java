package com.dongsoop.dongsoop.home.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class HomeThreadConfig {

    @Value("${home.thread.pool.size:10}")
    private Integer fixedThreadPoolSize;

    @Bean
    public ExecutorService homeThreadExecutor() {
        return Executors.newFixedThreadPool(fixedThreadPoolSize);
    }
}
