package com.dongsoop.dongsoop.monitoring.client;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.ArrayPercentilesItem;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.dongsoop.dongsoop.monitoring.dto.UsageWindow;
import com.dongsoop.dongsoop.monitoring.dto.UsageWindow.EndpointLatency;
import com.dongsoop.dongsoop.monitoring.dto.UsageWindow.FeatureCount;
import com.dongsoop.dongsoop.monitoring.service.ApiUsageRecorder;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** api-usage-* 인덱스를 한 번의 집계 요청으로 읽어 {@link UsageWindow} 로 바꾼다. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "monitoring.usage.enabled", havingValue = "true")
public class UsageStatsClient {

    private static final String ANONYMOUS_ACTOR = "anon";
    private static final int FEATURE_BUCKETS = 100;
    private static final int URI_BUCKETS = 300;
    private static final double P95 = 95.0;

    private final ElasticsearchClient client;

    public UsageWindow query(ZonedDateTime from, ZonedDateTime to) throws IOException {
        SearchResponse<Void> response = client.search(s -> s
                        .index(ApiUsageRecorder.INDEX_PREFIX + "*")
                        .size(0)
                        .trackTotalHits(t -> t.enabled(true))
                        .query(q -> q.range(r -> r.date(d -> d.field("@timestamp")
                                .gte(from.toOffsetDateTime().toString())
                                .lt(to.toOffsetDateTime().toString()))))
                        .aggregations("users", a -> a
                                .filter(f -> f.bool(b -> b.mustNot(m -> m.term(t -> t.field("actor").value(ANONYMOUS_ACTOR)))))
                                .aggregations("count", a2 -> a2.cardinality(c -> c.field("actor"))))
                        .aggregations("features", a -> a
                                .terms(t -> t.field("feature").size(FEATURE_BUCKETS))
                                .aggregations("users", a2 -> a2
                                        .filter(f -> f.bool(b -> b.mustNot(m -> m.term(t -> t.field("actor").value(ANONYMOUS_ACTOR)))))
                                        .aggregations("count", a3 -> a3.cardinality(c -> c.field("actor")))))
                        .aggregations("uris", a -> a
                                .terms(t -> t.field("uri").size(URI_BUCKETS))
                                .aggregations("p95", a2 -> a2.percentiles(p -> p.field("durationMs").percents(P95).keyed(false))))
                        .aggregations("errors", a -> a
                                .filter(f -> f.range(r -> r.number(n -> n.field("status").gte(500.0))))
                                .aggregations("uris", a2 -> a2.terms(t -> t.field("uri").size(1)))),
                Void.class);

        Map<String, Aggregate> aggs = response.aggregations();
        long calls = response.hits().total() == null ? 0 : response.hits().total().value();
        long users = identifiedUsers(aggs.get("users"));

        Map<String, FeatureCount> features = new LinkedHashMap<>();
        for (StringTermsBucket bucket : aggs.get("features").sterms().buckets().array()) {
            features.put(bucket.key().stringValue(),
                    new FeatureCount(bucket.docCount(), identifiedUsers(bucket.aggregations().get("users"))));
        }

        List<EndpointLatency> slowest = aggs.get("uris").sterms().buckets().array().stream()
                .map(bucket -> new EndpointLatency(bucket.key().stringValue(),
                        p95Of(bucket.aggregations().get("p95"))))
                .sorted(Comparator.comparingDouble(EndpointLatency::p95Ms).reversed())
                .toList();

        Aggregate errors = aggs.get("errors");
        long serverErrors = errors.filter().docCount();
        List<StringTermsBucket> errorUris = errors.filter().aggregations().get("uris").sterms().buckets().array();
        String topErrorUri = errorUris.isEmpty() ? null : errorUris.get(0).key().stringValue();

        return new UsageWindow(calls, users, features, slowest, serverErrors, topErrorUri);
    }

    private static long identifiedUsers(Aggregate filterAggregate) {
        return filterAggregate.filter().aggregations().get("count").cardinality().value();
    }

    private static double p95Of(Aggregate percentiles) {
        List<ArrayPercentilesItem> items = percentiles.tdigestPercentiles().values().array();
        if (items.isEmpty()) {
            return 0;
        }
        return items.get(0).value();
    }
}
