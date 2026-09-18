package com.dongsoop.dongsoop.monitoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import com.dongsoop.dongsoop.monitoring.dto.ApiUsageEvent;
import com.dongsoop.dongsoop.monitoring.service.ApiUsageRecorder;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

class ApiUsageRecorderTest {

    private static final ZonedDateTime AT = ZonedDateTime.of(2026, 9, 18, 10, 0, 0, 0, ZoneId.of("Asia/Seoul"));

    private final ElasticsearchClient client = mock(ElasticsearchClient.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<ElasticsearchClient> provider = mock(ObjectProvider.class);
    private final ApiUsageRecorder recorder = new ApiUsageRecorder(provider);

    private static ApiUsageEvent event(int i) {
        return new ApiUsageEvent(AT, "GET", "/home", "home", 200, i, null, null, "anon");
    }

    @Test
    @DisplayName("인덱스 이름은 문서 시각의 연월을 따른다")
    void indexNameFollowsMonth() {
        assertThat(ApiUsageRecorder.indexNameOf(event(1))).isEqualTo("api-usage-2026.09");
    }

    @Test
    @DisplayName("큐가 가득 차면 새 이벤트는 버리고 false")
    void dropsWhenQueueIsFull() {
        IntStream.range(0, ApiUsageRecorder.QUEUE_CAPACITY).forEach(i -> assertThat(recorder.offer(event(i))).isTrue());

        assertThat(recorder.offer(event(-1))).isFalse();
        assertThat(recorder.queued()).isEqualTo(ApiUsageRecorder.QUEUE_CAPACITY);
    }

    @Test
    @DisplayName("flush는 큐를 비워 bulk 한 번으로 보낸다")
    void flushSendsOneBulk() throws IOException {
        when(provider.getIfAvailable()).thenReturn(client);
        BulkResponse response = mock(BulkResponse.class);
        when(response.errors()).thenReturn(false);
        when(client.bulk(any(BulkRequest.class))).thenReturn(response);
        recorder.offer(event(1));
        recorder.offer(event(2));

        recorder.flush();

        ArgumentCaptor<BulkRequest> captor = ArgumentCaptor.forClass(BulkRequest.class);
        verify(client).bulk(captor.capture());
        assertThat(captor.getValue().operations()).hasSize(2);
        assertThat(recorder.queued()).isZero();
    }

    @Test
    @DisplayName("bulk 실패는 삼키고 그 묶음은 버린다")
    void flushSwallowsFailure() throws IOException {
        when(provider.getIfAvailable()).thenReturn(client);
        when(client.bulk(any(BulkRequest.class))).thenThrow(new IOException("down"));
        recorder.offer(event(1));

        recorder.flush();

        assertThat(recorder.queued()).isZero();
    }

    @Test
    @DisplayName("ES 클라이언트가 없으면 flush는 아무것도 하지 않는다")
    void flushWithoutClient() {
        when(provider.getIfAvailable()).thenReturn(null);
        recorder.offer(event(1));

        recorder.flush();

        assertThat(recorder.queued()).isEqualTo(1);
    }

    @Test
    @DisplayName("문서에는 null 인 회원·기기 ID를 넣지 않는다")
    void documentOmitsNullIds() {
        assertThat(event(1).toDocument()).containsKeys("@timestamp", "method", "uri", "feature", "status", "durationMs", "actor")
                .doesNotContainKeys("memberId", "deviceId");
    }
}
