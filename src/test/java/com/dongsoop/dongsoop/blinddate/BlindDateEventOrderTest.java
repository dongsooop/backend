package com.dongsoop.dongsoop.blinddate;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.blinddate.dto.StartBlindDateRequest;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateInfoRepositoryImpl;
import com.dongsoop.dongsoop.blinddate.repository.ParticipantInfoRepositoryImpl;
import com.dongsoop.dongsoop.blinddate.repository.SessionInfoRepository;
import com.dongsoop.dongsoop.blinddate.service.BlindDateService;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

/**
 * BlindDate 이벤트 순서 검증 테스트 메시지가 올바른 순서로 전달되는지 검증
 */
@Disabled("WebSocket 통합 테스트는 실제 서버 환경에서 실행. 단위 테스트로 대체됨")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        classes = WebSocketTestConfig.class,
        properties = {
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration"
        }
)
@Import(WebSocketTestConfig.class)
@ActiveProfiles("test")
@DisplayName("과팅 이벤트 순서 검증 테스트")
class BlindDateEventOrderTest {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(
            BlindDateEventOrderTest.class);

    private static final String WS_URL = "http://localhost:8080/ws/blinddate";

    @Autowired
    private BlindDateService blindDateService;
    @Autowired
    private BlindDateInfoRepositoryImpl blindDateInfoRepository;
    @Autowired
    private SessionInfoRepository sessionInfoRepository;
    @Autowired
    private ParticipantInfoRepositoryImpl participantInfoRepository;
    @Autowired
    private TestJwtTokenGenerator tokenGenerator;

    private WebSocketStompClient stompClient;

    private String generateTestToken(Long memberId) {
        return tokenGenerator.generateAccessToken(memberId);
    }

    @BeforeEach
    void setUp() {
        List<Transport> transports = new ArrayList<>();
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);

        stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    @AfterEach
    void tearDown() {
        if (blindDateInfoRepository.isAvailable()) {
            blindDateInfoRepository.close();
        }
    }

    @Test
    @DisplayName("이벤트 순서 검증: JOIN → JOINED → START → FREEZE → SYSTEM 메시지들")
    void verifyEventOrder_CorrectSequence() throws Exception {
        // 과팅 시작
        LocalDateTime expiredDate = LocalDateTime.now().plusHours(1);
        StartBlindDateRequest request = new StartBlindDateRequest(expiredDate, 2);
        blindDateService.startBlindDate(request);

        // 이벤트 순서 추적 핸들러
        OrderTrackingHandler handler1 = new OrderTrackingHandler(1L);
        OrderTrackingHandler handler2 = new OrderTrackingHandler(2L);

        // User1 연결
        WebSocketHttpHeaders headers1 = new WebSocketHttpHeaders();
        headers1.add("Authorization", "Bearer " + generateTestToken(1L));
        StompSession session1 = stompClient.connectAsync(WS_URL, headers1, handler1)
                .get(5, TimeUnit.SECONDS);

        // 0.5초 대기 후 User2 연결 (입장 순서 명확히)
        Thread.sleep(500);

        WebSocketHttpHeaders headers2 = new WebSocketHttpHeaders();
        headers2.add("Authorization", "Bearer " + generateTestToken(2L));
        StompSession session2 = stompClient.connectAsync(WS_URL, headers2, handler2)
                .get(5, TimeUnit.SECONDS);

        // 충분한 시간 대기 (안내 메시지 7개 + 여유)
        Thread.sleep(20000);

        // ===== 순서 검증 =====
        log.info("========================================");
        log.info("  이벤트 순서 검증");
        log.info("========================================\n");

        // User1 이벤트 순서
        List<String> user1Order = handler1.getEventOrder();
        log.info("[User1] 이벤트 순서: {}", user1Order);

        // User2 이벤트 순서
        List<String> user2Order = handler2.getEventOrder();
        log.info("[User2] 이벤트 순서: {}\n", user2Order);

        // ===== User1 검증 =====
        assertThat(user1Order).isNotEmpty();

        // JOIN은 첫 번째
        assertThat(user1Order.get(0)).isEqualTo("JOIN");
        log.info("✅ User1: JOIN이 첫 번째");

        // JOINED는 JOIN 다음
        int user1JoinIndex = user1Order.indexOf("JOIN");
        int user1JoinedIndex = user1Order.indexOf("JOINED");
        assertThat(user1JoinedIndex).isGreaterThan(user1JoinIndex);
        log.info("✅ User1: JOINED가 JOIN 다음");

        // START는 JOINED 다음
        int user1StartIndex = user1Order.indexOf("START");
        assertThat(user1StartIndex).isGreaterThan(user1JoinedIndex);
        log.info("✅ User1: START가 JOINED 다음");

        // FREEZE는 START 다음
        int user1FreezeIndex = user1Order.indexOf("FREEZE");
        assertThat(user1FreezeIndex).isGreaterThan(user1StartIndex);
        log.info("✅ User1: FREEZE가 START 다음");

        // SYSTEM 메시지는 FREEZE 다음
        int user1FirstSystemIndex = user1Order.indexOf("SYSTEM");
        assertThat(user1FirstSystemIndex).isGreaterThan(user1FreezeIndex);
        log.info("✅ User1: SYSTEM 메시지가 FREEZE 다음");

        // SYSTEM 메시지는 THAW 전에 여러 개
        int user1ThawIndex = user1Order.indexOf("THAW");
        long systemCountBeforeThaw = user1Order.subList(user1FreezeIndex, user1ThawIndex)
                .stream()
                .filter(e -> e.equals("SYSTEM"))
                .count();
        assertThat(systemCountBeforeThaw).isGreaterThanOrEqualTo(7); // 안내 메시지 7개
        log.info("✅ User1: FREEZE와 THAW 사이에 SYSTEM 메시지 {}개", systemCountBeforeThaw);

        // ===== User2 검증 =====
        assertThat(user2Order).isNotEmpty();

        // JOIN은 첫 번째
        assertThat(user2Order.get(0)).isEqualTo("JOIN");
        log.info("✅ User2: JOIN이 첫 번째");

        // JOINED는 JOIN 다음
        int user2JoinIndex = user2Order.indexOf("JOIN");
        int user2JoinedIndex = user2Order.indexOf("JOINED");
        assertThat(user2JoinedIndex).isGreaterThan(user2JoinIndex);
        log.info("✅ User2: JOINED가 JOIN 다음");

        // START는 JOINED 다음
        int user2StartIndex = user2Order.indexOf("START");
        assertThat(user2StartIndex).isGreaterThan(user2JoinedIndex);
        log.info("✅ User2: START가 JOINED 다음");

        // FREEZE는 START 다음
        int user2FreezeIndex = user2Order.indexOf("FREEZE");
        assertThat(user2FreezeIndex).isGreaterThan(user2StartIndex);
        log.info("✅ User2: FREEZE가 START 다음");

        // SYSTEM 메시지는 FREEZE 다음
        int user2FirstSystemIndex = user2Order.indexOf("SYSTEM");
        assertThat(user2FirstSystemIndex).isGreaterThan(user2FreezeIndex);
        log.info("✅ User2: SYSTEM 메시지가 FREEZE 다음\n");

        // ===== 상세 검증 =====
        log.info("========================================");
        log.info("  상세 이벤트 타임라인");
        log.info("========================================\n");

        log.info("[User1] 상세:");
        handler1.printDetailedTimeline();

        log.info("\n[User2] 상세:");
        handler2.printDetailedTimeline();

        log.info("\n🎉 모든 이벤트 순서 검증 완료!");

        session1.disconnect();
        session2.disconnect();
    }

    /**
     * 이벤트 순서 추적 핸들러
     */
    static class OrderTrackingHandler extends StompSessionHandlerAdapter {
        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OrderTrackingHandler.class);
        private final Long memberId;
        private final List<TimestampedEvent> events = new CopyOnWriteArrayList<>();
        private String sessionId;

        public OrderTrackingHandler(Long memberId) {
            this.memberId = memberId;
        }

        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            // JOIN 이벤트 구독
            session.subscribe("/user/queue/blinddate/join", new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return Map.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    Map<String, Object> data = (Map<String, Object>) payload;
                    sessionId = (String) data.get("sessionId");
                    recordEvent("JOIN", data);
                    subscribeToSession(session, sessionId);
                }
            });
        }

        private void subscribeToSession(StompSession session, String sessionId) {
            String base = "/topic/blinddate/session/" + sessionId;

            // 모든 이벤트 구독
            session.subscribe(base + "/joined", createHandler("JOINED"));
            session.subscribe(base + "/start", createHandler("START"));
            session.subscribe(base + "/freeze", createHandler("FREEZE"));
            session.subscribe(base + "/thaw", createHandler("THAW"));
            session.subscribe(base + "/system", createHandler("SYSTEM"));
            session.subscribe(base + "/message", createHandler("MESSAGE"));
            session.subscribe(base + "/participants", createHandler("PARTICIPANTS"));
        }

        private StompFrameHandler createHandler(String eventType) {
            return new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return Map.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    Map<String, Object> data = payload instanceof Map ? (Map<String, Object>) payload : Map.of();
                    recordEvent(eventType, data);
                }
            };
        }

        private void recordEvent(String eventType, Map<String, Object> data) {
            events.add(new TimestampedEvent(eventType, data, System.currentTimeMillis()));
            log.debug("[User{}] Event received: {} at {}", memberId, eventType, System.currentTimeMillis());
        }

        public List<String> getEventOrder() {
            return events.stream()
                    .map(e -> e.type)
                    .toList();
        }

        public void printDetailedTimeline() {
            long startTime = events.isEmpty() ? 0 : events.get(0).timestamp;
            events.forEach(event -> {
                long relativeTime = event.timestamp - startTime;
                log.info("  +{:5d}ms: {}", relativeTime, event.type);
            });
        }
    }

    /**
     * 타임스탬프가 포함된 이벤트
     */
    static class TimestampedEvent {
        String type;
        Map<String, Object> data;
        long timestamp;

        TimestampedEvent(String type, Map<String, Object> data, long timestamp) {
            this.type = type;
            this.data = data;
            this.timestamp = timestamp;
        }
    }
}
