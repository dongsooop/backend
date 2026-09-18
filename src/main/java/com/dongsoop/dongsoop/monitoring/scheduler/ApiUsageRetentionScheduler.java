package com.dongsoop.dongsoop.monitoring.scheduler;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.dongsoop.dongsoop.monitoring.service.ApiUsageRecorder;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "monitoring.usage.enabled", havingValue = "true")
public class ApiUsageRetentionScheduler {

    private static final DateTimeFormatter INDEX_MONTH = DateTimeFormatter.ofPattern("yyyy.MM");
    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    private final ObjectProvider<ElasticsearchClient> clientProvider;
    private final int retentionMonths;

    /** 0 이하면 삭제하지 않고 계속 보관한다 */
    public ApiUsageRetentionScheduler(ObjectProvider<ElasticsearchClient> clientProvider,
                                      @Value("${monitoring.usage.retention-months:0}") int retentionMonths) {
        this.clientProvider = clientProvider;
        this.retentionMonths = retentionMonths;
    }

    @Scheduled(cron = "0 0 4 * * ?", zone = "Asia/Seoul")
    public void deleteExpiredIndices() {
        ElasticsearchClient client = clientProvider.getIfAvailable();
        if (client == null || retentionMonths <= 0) {
            return;
        }
        try {
            Set<String> indices = client.indices()
                    .get(g -> g.index(ApiUsageRecorder.INDEX_PREFIX + "*"))
                    .result().keySet();
            List<String> expired = expiredOf(indices, YearMonth.now(ZONE), retentionMonths);
            for (String index : expired) {
                client.indices().delete(d -> d.index(index));
                log.info("Deleted expired api usage index {}", index);
            }
        } catch (Exception e) {
            log.warn("Api usage retention failed: {}", e.getMessage());
        }
    }

    /** 현재 달에서 retentionMonths 를 뺀 달보다 오래된 인덱스만 고른다. 이름이 형식에 안 맞으면 건드리지 않는다. */
    public static List<String> expiredOf(Set<String> indices, YearMonth now, int retentionMonths) {
        YearMonth oldestKept = now.minusMonths(retentionMonths);
        return indices.stream()
                .filter(name -> name.startsWith(ApiUsageRecorder.INDEX_PREFIX))
                .filter(name -> {
                    try {
                        YearMonth month = YearMonth.parse(name.substring(ApiUsageRecorder.INDEX_PREFIX.length()),
                                INDEX_MONTH);
                        return month.isBefore(oldestKept);
                    } catch (DateTimeParseException e) {
                        return false;
                    }
                })
                .sorted()
                .toList();
    }
}
