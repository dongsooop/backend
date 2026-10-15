package com.dongsoop.dongsoop.monitoring;

import static org.assertj.core.api.Assertions.assertThat;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.dongsoop.dongsoop.monitoring.client.UsageStatsClient;
import com.dongsoop.dongsoop.monitoring.dto.UsageWindow;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.Duration;
import java.util.Map;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class UsageAggregationIntegrationTest {
    private static final String INDEX = "api-usage-2026.10";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    @Container
    static final GenericContainer<?> elasticsearch = new GenericContainer<>("docker.elastic.co/elasticsearch/elasticsearch:8.17.3")
            .withEnv("discovery.type", "single-node").withEnv("xpack.security.enabled", "false")
            .withEnv("ES_JAVA_OPTS", "-Xms256m -Xmx256m").withExposedPorts(9200)
            .waitingFor(Wait.forHttp("/").forStatusCode(200)).withStartupTimeout(Duration.ofMinutes(3));
    private static RestClientTransport transport;
    private static ElasticsearchClient client;
    private UsageStatsClient service;

    @BeforeAll
    static void connect() {
        RestClient rest = RestClient.builder(new HttpHost(elasticsearch.getHost(), elasticsearch.getMappedPort(9200))).build();
        transport = new RestClientTransport(rest, new JacksonJsonpMapper());
        client = new ElasticsearchClient(transport);
    }
    @AfterAll
    static void close() throws Exception { if (transport != null) { transport.close(); } }
    @BeforeEach
    void reset() throws Exception {
        if (client.indices().exists(e -> e.index(INDEX)).value()) { client.indices().delete(d -> d.index(INDEX)); }
        client.indices().create(c -> c.index(INDEX).mappings(m -> m
                .properties("@timestamp", p -> p.date(d -> d))
                .properties("actor", p -> p.keyword(k -> k)).properties("feature", p -> p.keyword(k -> k))
                .properties("uri", p -> p.keyword(k -> k)).properties("status", p -> p.integer(i -> i))
                .properties("durationMs", p -> p.long_(l -> l))));
        service = new UsageStatsClient(client);
    }

    @Test
    void realAggregationsKeepTimeBoundaryUserExclusionsAndEndpointStatistics() throws Exception {
        event("2026-10-14T09:00:00+09:00", "m:7", "meal", "/meal", 200, 100);
        event("2026-10-14T09:20:00+09:00", "m:7", "meal", "/meal", 500, 200);
        event("2026-10-14T10:00:00+09:00", "d:9", "chat", "/chat", 503, 300);
        event("2026-10-15T00:00:00+09:00", "m:8", "chat", "/chat", 200, 400);
        event("2026-10-15T00:10:00+09:00", "admin", "ops", "/admin", 200, 500);
        event("2026-10-15T00:20:00+09:00", "anon", "meal", "/meal", 200, 600);
        event("2026-10-16T00:00:00+09:00", "m:99", "other", "/excluded", 500, 900);
        client.indices().refresh(r -> r.index(INDEX));
        UsageWindow result = service.query(day(14), day(16));
        assertThat(result.calls()).isEqualTo(6);
        assertThat(result.users()).isEqualTo(3);
        assertThat(result.memberUsers()).isEqualTo(2);
        assertThat(result.features()).containsEntry("meal", new UsageWindow.FeatureCount(3, 1))
                .containsEntry("chat", new UsageWindow.FeatureCount(2, 2))
                .containsEntry("ops", new UsageWindow.FeatureCount(1, 0)).doesNotContainKey("other");
        assertThat(result.dailyUsers()).containsEntry(LocalDate.of(2026, 10, 14), 2L)
                .containsEntry(LocalDate.of(2026, 10, 15), 1L);
        assertThat(result.peakHour()).isEqualTo(new UsageWindow.HourCount(4, 0, 3));
        assertThat(result.topByCalls()).containsExactly(new UsageWindow.EndpointCount("/meal", 3),
                new UsageWindow.EndpointCount("/chat", 2), new UsageWindow.EndpointCount("/admin", 1));
        assertThat(result.slowest()).extracting(UsageWindow.EndpointLatency::uri).containsExactly("/meal", "/admin", "/chat");
        assertThat(result.p95Ms()).isBetween(500.0, 600.0);
        assertThat(result.serverErrors()).isEqualTo(2);
        assertThat(result.errorsByUri()).containsExactlyInAnyOrder(new UsageWindow.EndpointCount("/meal", 1),
                new UsageWindow.EndpointCount("/chat", 1));
    }

    @Test
    void emptyWindowHasNoCallsUsersFeaturesOrPeak() throws Exception {
        UsageWindow result = service.query(day(14), day(15));
        assertThat(result.calls()).isZero();
        assertThat(result.users()).isZero();
        assertThat(result.memberUsers()).isZero();
        assertThat(result.features()).isEmpty();
        assertThat(result.dailyUsers()).isEmpty();
        assertThat(result.topByCalls()).isEmpty();
        assertThat(result.slowest()).isEmpty();
        assertThat(result.errorsByUri()).isEmpty();
        assertThat(result.peakHour()).isNull();
    }

    private ZonedDateTime day(int day) { return LocalDate.of(2026, 10, day).atStartOfDay(KST); }
    private void event(String time, String actor, String feature, String uri, int status, long duration) throws Exception {
        client.index(i -> i.index(INDEX).document(Map.of("@timestamp", time, "actor", actor,
                "feature", feature, "uri", uri, "status", status, "durationMs", duration)));
    }
}
