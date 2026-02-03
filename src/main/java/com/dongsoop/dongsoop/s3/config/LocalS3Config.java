package com.dongsoop.dongsoop.s3.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@Profile("local")
@Slf4j
public class LocalS3Config {

    @Bean
    @Primary
    public AwsCredentialsProvider customAwsCredentialsProvider() {
        log.info("Called LocalS3Config.customAwsCredentialsProvider - returning null for local profile.");
        return null;
    }

    @Bean
    @Primary
    public S3Client s3Client() {
        log.info("Called LocalS3Config.s3Client - returning null for local profile.");
        return null;
    }

    @Bean
    public AwsRegionProvider customAwsRegionProvider() {
        log.info("Called LocalS3Config.customAwsRegionProvider - returning null for local profile.");
        return null;
    }
}
