package com.dongsoop.dongsoop.monitoring.client;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.ArrayPercentilesItem;
import co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval;
import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramBucket;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.dongsoop.dongsoop.monitoring.dto.UsageWindow;
import com.dongsoop.dongsoop.monitoring.dto.UsageWindow.EndpointCount;
import com.dongsoop.dongsoop.monitoring.dto.UsageWindow.EndpointLatency;
import com.dongsoop.dongsoop.monitoring.dto.UsageWindow.FeatureCount;
import com.dongsoop.dongsoop.monitoring.dto.UsageWindow.HourCount;
import com.dongsoop.dongsoop.monitoring.service.ApiUsageRecorder;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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

    /** 사용자 수에서 빼는 actor: 식별 불가(anon)와 관리자(admin) */
    private static final List<co.elastic.clients.elasticsearch._types.FieldValue> EXCLUDED_ACTORS = List.of(
            co.elastic.clients.elasticsearch._types.FieldValue.of("anon"),
            co.elastic.clients.elasticsearch._types.FieldValue.of("admin"));
    private static final String MEMBER_ACTOR_PREFIX = "m:";
    private static final String ZONE_ID = "Asia/Seoul";
    private static final ZoneId ZONE = ZoneId.of(ZONE_ID);
    private static final int FEATURE_BUCKETS = 100;
    private static final int URI_BUCKETS = 300;
    private static final int TOP_CALLS = 5;
    private static final int TOP_ERRORS = 3;
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
                        .aggregations("users", identifiedUsers())
                        .aggregations("members", a -> a
                                .filter(f -> f.prefix(p -> p.field("actor").value(MEMBER_ACTOR_PREFIX)))
                                .aggregations("count", a2 -> a2.cardinality(c -> c.field("actor"))))
                        .aggregations("p95", a -> a.percentiles(p -> p.field("durationMs").percents(P95).keyed(false)))
                        .aggregations("features", a -> a
                                .terms(t -> t.field("feature").size(FEATURE_BUCKETS))
                                .aggregations("users", identifiedUsers()))
                        .aggregations("daily", a -> a
                                .dateHistogram(h -> h.field("@timestamp").calendarInterval(CalendarInterval.Day)
                                        .timeZone(ZONE_ID).minDocCount(0))
                                .aggregations("users", identifiedUsers()))
                        .aggregations("hourly", a -> a
                                .dateHistogram(h -> h.field("@timestamp").calendarInterval(CalendarInterval.Hour)
                                        .timeZone(ZONE_ID).minDocCount(1)))
                        .aggregations("uris", a -> a
                                .terms(t -> t.field("uri").size(URI_BUCKETS))
                                .aggregations("p95", a2 -> a2.percentiles(p -> p.field("durationMs").percents(P95).keyed(false))))
                        .aggregations("errors", a -> a
                                .filter(f -> f.range(r -> r.number(n -> n.field("status").gte(500.0))))
                                .aggregations("uris", a2 -> a2.terms(t -> t.field("uri").size(TOP_ERRORS)))),
                Void.class);

        Map<String, Aggregate> aggs = response.aggregations();
        long calls = response.hits().total() == null ? 0 : response.hits().total().value();

        Map<String, FeatureCount> features = new LinkedHashMap<>();
        for (StringTermsBucket bucket : aggs.get("features").sterms().buckets().array()) {
            features.put(bucket.key().stringValue(),
                    new FeatureCount(bucket.docCount(), usersOf(bucket.aggregations().get("users"))));
        }

        Map<LocalDate, Long> dailyUsers = new LinkedHashMap<>();
        for (DateHistogramBucket bucket : aggs.get("daily").dateHistogram().buckets().array()) {
            LocalDate day = Instant.ofEpochMilli(bucket.key()).atZone(ZONE).toLocalDate();
            dailyUsers.put(day, usersOf(bucket.aggregations().get("users")));
        }

        HourCount peak = null;
        for (DateHistogramBucket bucket : aggs.get("hourly").dateHistogram().buckets().array()) {
            if (peak == null || bucket.docCount() > peak.count()) {
                ZonedDateTime at = Instant.ofEpochMilli(bucket.key()).atZone(ZONE);
                peak = new HourCount(at.getDayOfWeek().getValue(), at.getHour(), bucket.docCount());
            }
        }

        List<StringTermsBucket> uriBuckets = aggs.get("uris").sterms().buckets().array();
        List<EndpointCount> topByCalls = uriBuckets.stream()
                .map(b -> new EndpointCount(b.key().stringValue(), b.docCount()))
                .sorted(Comparator.comparingLong(EndpointCount::count).reversed())
                .limit(TOP_CALLS)
                .toList();
        List<EndpointLatency> slowest = uriBuckets.stream()
                .map(b -> new EndpointLatency(b.key().stringValue(), p95Of(b.aggregations().get("p95"))))
                .sorted(Comparator.comparingDouble(EndpointLatency::p95Ms).reversed())
                .toList();

        Aggregate errors = aggs.get("errors");
        List<EndpointCount> errorsByUri = errors.filter().aggregations().get("uris").sterms().buckets().array().stream()
                .map(b -> new EndpointCount(b.key().stringValue(), b.docCount()))
                .toList();

        return new UsageWindow(
                calls,
                usersOf(aggs.get("users")),
                aggs.get("members").filter().aggregations().get("count").cardinality().value(),
                p95Of(aggs.get("p95")),
                features,
                dailyUsers,
                topByCalls,
                slowest,
                errors.filter().docCount(),
                errorsByUri,
                peak
        );
    }

    /** anon·admin 을 뺀 actor 의 cardinality. 비회원 기기·fid 는 포함된다 */
    private static Aggregation identifiedUsers() {
        return Aggregation.of(a -> a
                .filter(f -> f.bool(b -> b.mustNot(m -> m.terms(t -> t.field("actor").terms(v -> v.value(EXCLUDED_ACTORS))))))
                .aggregations("count", a2 -> a2.cardinality(c -> c.field("actor"))));
    }

    private static long usersOf(Aggregate filterAggregate) {
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
