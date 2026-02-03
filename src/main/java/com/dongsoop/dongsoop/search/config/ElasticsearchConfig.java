package com.dongsoop.dongsoop.search.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Slf4j
@Configuration
@ConditionalOnProperty(
        name = "spring.data.elasticsearch.repositories.enabled",
        havingValue = "true",
        matchIfMissing = true
)
@EnableElasticsearchRepositories(basePackages = "com.dongsoop.dongsoop.search.repository")
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris}")
    private String elasticsearchUrl;

    private ElasticsearchClient client;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        RestClient restClient = createRestClient();
        ElasticsearchTransport transport = createTransport(restClient);
        this.client = new ElasticsearchClient(transport);
        return this.client;
    }

    @PostConstruct
    public void initializeConnection() {
        performConnectionTest();
    }

    private RestClient createRestClient() {
        return RestClient.builder(HttpHost.create(elasticsearchUrl)).build();
    }

    private ElasticsearchTransport createTransport(RestClient restClient) {
        return new RestClientTransport(restClient, new JacksonJsonpMapper());
    }

    private void performConnectionTest() {
        if (client == null) {
            logClientNotInitialized();
            return;
        }

        testElasticsearchConnection();
    }

    private void testElasticsearchConnection() {
        try {
            client.info();
            logConnectionSuccess();
        } catch (Exception e) {
            logConnectionFailure(e);
        }
    }

    private void logClientNotInitialized() {
        log.warn("Elasticsearch client is not initialized");
    }

    private void logConnectionSuccess() {
        log.info("Elasticsearch connection initialized successfully");
    }

    private void logConnectionFailure(Exception e) {
        log.error("Failed to initialize Elasticsearch connection - URL: {}, Error: {}",
                elasticsearchUrl, e.getMessage(), e);
    }
}