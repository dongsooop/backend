package com.dongsoop.dongsoop.monitoring;

import static org.assertj.core.api.Assertions.assertThat;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.dongsoop.dongsoop.monitoring.client.UsageStatsClient;
import com.dongsoop.dongsoop.monitoring.dto.UsageReport;
import com.dongsoop.dongsoop.monitoring.dto.UsageWindow;
import com.dongsoop.dongsoop.monitoring.service.UsageReportServiceImpl;
import com.dongsoop.dongsoop.monitoring.util.UsageReportMessage;
import java.time.LocalDate;
import java.time.ZoneId;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * 로컬 ES(localhost:9200)에 api-usage-* 문서가 있을 때만 수동으로 돌리는 확인용 테스트.
 * 실행: ES_LOCAL=1 ./gradlew test --tests '*UsageStatsClientLocalIT'
 */
@EnabledIfEnvironmentVariable(named = "ES_LOCAL", matches = ".+")
class UsageStatsClientLocalIT {

    @Test
    @DisplayName("집계 쿼리가 실제 ES에서 파싱되고 리포트 문장이 만들어진다")
    void queriesRealElasticsearch() throws Exception {
        RestClient restClient = RestClient.builder(HttpHost.create("http://localhost:9200")).build();
        ElasticsearchClient client = new ElasticsearchClient(new RestClientTransport(restClient, new JacksonJsonpMapper()));
        UsageStatsClient statsClient = new UsageStatsClient(client);
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

        UsageWindow window = statsClient.query(today.atStartOfDay(ZoneId.of("Asia/Seoul")),
                today.plusDays(1).atStartOfDay(ZoneId.of("Asia/Seoul")));
        UsageReport report = new UsageReportServiceImpl(statsClient).build(today.minusDays(6), today.plusDays(1));
        String message = UsageReportMessage.format(report);
        System.out.println("=== window: " + window);
        System.out.println(message);

        assertThat(window.calls()).isGreaterThan(0);
        assertThat(window.features()).containsKey("meal");
        assertThat(window.users()).isEqualTo(1);
        assertThat(message).contains("학식");
        restClient.close();
    }
}
