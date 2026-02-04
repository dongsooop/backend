package com.dongsoop.dongsoop.s3.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.providers.AwsRegionProvider;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@Profile("prod")
public class S3Config {

    @Value("${cloud.aws.credentials.access-key}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key}")
    private String secretKey;

    @Value("${cloud.aws.region.static}")
    private String region;

    @Value("${cloud.aws.s3.endpoint}")
    private String endpoint;

    @Bean
    @Primary
    public AwsCredentialsProvider customAwsCredentialsProvider() {
        return () -> new AwsCredentials() {
            @Override
            public String accessKeyId() {
                return accessKey;
            }

            @Override
            public String secretAccessKey() {
                return secretKey;
            }
        };
    }

    @Bean
    @Primary
    public S3Client s3Client() {
        URI endpointUri;
        try {
            endpointUri = URI.create(endpoint);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("OCI Endpoint 설정이 올바르지 않습니다: " + endpoint, e);
        }

        return S3Client.builder()
                .credentialsProvider(customAwsCredentialsProvider())
                .region(Region.of(region))
                .endpointOverride(endpointUri)
                .forcePathStyle(true)
                .build();
    }

    @Bean
    public AwsRegionProvider customAwsRegionProvider() {
        return () -> Region.of(region);
    }
}
