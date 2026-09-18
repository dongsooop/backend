package com.dongsoop.dongsoop.monitoring.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import com.dongsoop.dongsoop.monitoring.dto.ApiUsageEvent;
import jakarta.annotation.PostConstruct;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 요청 기록을 메모리 큐에 모았다가 5초마다 ES에 bulk로 쓴다.
 * 어떤 실패도 요청 처리에 영향을 주면 안 되므로 큐가 차면 버리고, bulk 실패도 버린다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "monitoring.usage.enabled", havingValue = "true")
public class ApiUsageRecorder {

    public static final String INDEX_PREFIX = "api-usage-";
    static final String INDEX_TEMPLATE_NAME = "api-usage";
    public static final int QUEUE_CAPACITY = 10_000;
    private static final int FLUSH_BATCH = 5_000;
    private static final long DROP_LOG_INTERVAL_MS = 60_000;
    private static final DateTimeFormatter INDEX_MONTH = DateTimeFormatter.ofPattern("yyyy.MM");

    private final ObjectProvider<ElasticsearchClient> clientProvider;
    private final LinkedBlockingQueue<ApiUsageEvent> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private final AtomicLong lastDropLogAt = new AtomicLong();

    public ApiUsageRecorder(ObjectProvider<ElasticsearchClient> clientProvider) {
        this.clientProvider = clientProvider;
    }

    @PostConstruct
    void ensureIndexTemplate() {
        ElasticsearchClient client = clientProvider.getIfAvailable();
        if (client == null) {
            log.warn("ElasticsearchClient bean is absent; api usage recording is disabled");
            return;
        }
        try {
            if (client.indices().existsIndexTemplate(e -> e.name(INDEX_TEMPLATE_NAME)).value()) {
                return;
            }
            client.indices().putIndexTemplate(t -> t
                    .name(INDEX_TEMPLATE_NAME)
                    .indexPatterns(INDEX_PREFIX + "*")
                    // 단일 노드 ES라 복제본을 두면 인덱스가 계속 yellow 로 남는다
                    .template(tt -> tt
                            .settings(st -> st.numberOfReplicas("0"))
                            .mappings(m -> m
                            .properties("@timestamp", p -> p.date(d -> d))
                            .properties("method", p -> p.keyword(k -> k))
                            .properties("uri", p -> p.keyword(k -> k))
                            .properties("feature", p -> p.keyword(k -> k))
                            .properties("status", p -> p.integer(i -> i))
                            .properties("durationMs", p -> p.long_(l -> l))
                            .properties("memberId", p -> p.long_(l -> l))
                            .properties("deviceId", p -> p.long_(l -> l))
                            .properties("actor", p -> p.keyword(k -> k)))));
            log.info("Registered index template {}", INDEX_TEMPLATE_NAME);
        } catch (Exception e) {
            log.warn("Failed to register api usage index template: {}", e.getMessage());
        }
    }

    public boolean offer(ApiUsageEvent event) {
        if (queue.offer(event)) {
            return true;
        }
        long now = System.currentTimeMillis();
        long last = lastDropLogAt.get();
        if (now - last > DROP_LOG_INTERVAL_MS && lastDropLogAt.compareAndSet(last, now)) {
            log.warn("Api usage queue is full ({}); dropping events", QUEUE_CAPACITY);
        }
        return false;
    }

    @Scheduled(fixedDelay = 5_000)
    public void flush() {
        ElasticsearchClient client = clientProvider.getIfAvailable();
        if (client == null || queue.isEmpty()) {
            return;
        }
        List<ApiUsageEvent> batch = new ArrayList<>(FLUSH_BATCH);
        queue.drainTo(batch, FLUSH_BATCH);
        try {
            BulkResponse response = client.bulk(bulkRequest(batch));
            if (response.errors()) {
                log.warn("Api usage bulk had item errors ({} events)", batch.size());
            }
        } catch (Exception e) {
            log.warn("Api usage bulk failed; dropped {} events: {}", batch.size(), e.getMessage());
        }
    }

    static BulkRequest bulkRequest(List<ApiUsageEvent> events) {
        List<BulkOperation> operations = events.stream()
                .map(event -> BulkOperation.of(op -> op.index(idx -> idx
                        .index(indexNameOf(event))
                        .document(event.toDocument()))))
                .toList();
        return BulkRequest.of(b -> b.operations(operations));
    }

    public static String indexNameOf(ApiUsageEvent event) {
        return INDEX_PREFIX + INDEX_MONTH.format(event.timestamp());
    }

    public int queued() {
        return queue.size();
    }
}
