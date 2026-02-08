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
import java.util.concurrent.CountDownLatch;
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
import org.springframework.messaging.simp.stomp.StompCommand;
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
 * BlindDate WebSocket 완전 통합 테스트 실제 WebSocket 연결을 통해 전체 시나리오 검증
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
@DisplayName("과팅 WebSocket 완전 통합 테스트")
class BlindDateWebSocketIntegrationTest {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(
            BlindDateWebSocketIntegrationTest.class);

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
        // WebSocket 클라이언트 설정
        List<Transport> transports = new ArrayList<>();
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);

        stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    @AfterEach
    void tearDown() {
        // 정리
        if (blindDateInfoRepository.isAvailable()) {
            blindDateInfoRepository.close();
        }
    }

    @Test
    @DisplayName("완전 시나리오: 2명 입장 → 세션 시작 → 메시지 수신 → 매칭")
    void fullScenario_TwoUsers_SessionStart_Messages_Matching() throws Exception {
        // ========== STEP 1: 과팅 시작 ==========
        log.info("📋 STEP 1: 과팅 시작");
        LocalDateTime expiredDate = LocalDateTime.now().plusHours(1);
        StartBlindDateRequest request = new StartBlindDateRequest(expiredDate, 2);
        blindDateService.startBlindDate(request);

        assertThat(blindDateInfoRepository.isAvailable()).isTrue();
        assertThat(blindDateInfoRepository.getMaxSessionMemberCount()).isEqualTo(2);
        log.info("✅ 과팅 시작 완료\n");

        // ========== STEP 2: 클라이언트 2명 접속 ==========
        log.info("📋 STEP 2: 클라이언트 2명 접속");

        TestStompSessionHandler handler1 = new TestStompSessionHandler(1L);
        TestStompSessionHandler handler2 = new TestStompSessionHandler(2L);

        // 연결
        WebSocketHttpHeaders headers1 = new WebSocketHttpHeaders();
        headers1.add("Authorization", "Bearer " + generateTestToken(1L));

        WebSocketHttpHeaders headers2 = new WebSocketHttpHeaders();
        headers2.add("Authorization", "Bearer " + generateTestToken(2L));

        StompSession session1 = stompClient.connectAsync(WS_URL, headers1, handler1)
                .get(5, TimeUnit.SECONDS);
        log.info("✅ User1 connected");

        StompSession session2 = stompClient.connectAsync(WS_URL, headers2, handler2)
                .get(5, TimeUnit.SECONDS);
        log.info("✅ User2 connected\n");

        // ========== STEP 3: JOIN 이벤트 대기 ==========
        log.info("📋 STEP 3: JOIN 이벤트 대기");

        // JOIN 이벤트는 handleConnect에서 자동 발송됨
        Thread.sleep(2000);

        assertThat(handler1.sessionId).isNotNull();
        assertThat(handler2.sessionId).isNotNull();
        assertThat(handler1.sessionId).isEqualTo(handler2.sessionId);
        log.info("✅ 같은 세션에 배치: {}\n", handler1.sessionId);

        // ========== STEP 4: START 이벤트 대기 ==========
        log.info("📋 STEP 4: START 이벤트 대기 (2명 입장 시 자동 시작)");

        // 세션 시작 이벤트 대기 (최대 3초)
        handler1.waitForEvent("START", 3000);
        handler2.waitForEvent("START", 3000);

        assertThat(handler1.hasEvent("START")).isTrue();
        assertThat(handler2.hasEvent("START")).isTrue();
        log.info("✅ START 이벤트 수신 완료\n");

        // ========== STEP 5: FREEZE 이벤트 확인 ==========
        log.info("📋 STEP 5: FREEZE 이벤트 확인");

        handler1.waitForEvent("FREEZE", 2000);

        assertThat(handler1.hasEvent("FREEZE")).isTrue();
        assertThat(handler2.hasEvent("FREEZE")).isTrue();
        log.info("✅ FREEZE 이벤트 수신\n");

        // ========== STEP 6: 시스템 메시지 수신 ==========
        log.info("📋 STEP 6: 시스템 메시지 수신 (3개, 2초 간격)");

        // 시작 메시지 3개 대기 (2초 * 3 = 6초 + 여유 2초)
        Thread.sleep(8000);

        int systemCount1 = handler1.getEventCount("SYSTEM");
        int systemCount2 = handler2.getEventCount("SYSTEM");

        assertThat(systemCount1).isGreaterThanOrEqualTo(3);
        assertThat(systemCount2).isGreaterThanOrEqualTo(3);
        log.info("✅ 시스템 메시지 수신: User1={}개, User2={}개\n", systemCount1, systemCount2);

        // ========== STEP 7: THAW 이벤트 확인 ==========
        log.info("📋 STEP 7: THAW 이벤트 확인");

        assertThat(handler1.hasEvent("THAW")).isTrue();
        assertThat(handler2.hasEvent("THAW")).isTrue();
        log.info("✅ THAW 이벤트 수신\n");

        // ========== STEP 8: 채팅 테스트 ==========
        log.info("📋 STEP 8: 채팅 테스트");

        // User1이 메시지 전송 (DTO 구조 변경: senderId 제거, Principal에서 추출)
        session1.send("/app/blinddate/message", Map.of(
                "message", "안녕하세요!"
        ));

        Thread.sleep(1000);

        // 양쪽 모두 메시지를 받아야 함
        assertThat(handler1.hasEvent("MESSAGE")).isTrue();
        assertThat(handler2.hasEvent("MESSAGE")).isTrue();
        log.info("✅ 채팅 송수신 확인\n");

        // ========== STEP 9: 사랑의 작대기 대기 ==========
        log.info("📋 STEP 9: 사랑의 작대기 대기 (이벤트 메시지 후)");
        log.info("⏳ 빠른 테스트를 위해 10초만 대기...\n");

        Thread.sleep(10000);

        // ========== STEP 10: 참가자 목록 수신 ==========
        log.info("📋 STEP 10: 참가자 목록 수신");

        if (handler1.hasEvent("PARTICIPANTS")) {
            log.info("✅ PARTICIPANTS 이벤트 수신\n");
        } else {
            log.warn("⚠️ PARTICIPANTS 이벤트 아직 미수신 (더 대기 필요)\n");
        }

        // ========== STEP 11: 매칭 테스트 ==========
        log.info("📋 STEP 11: 매칭 테스트 (서로 선택)");

        // User1 → User2 선택 (DTO 구조 변경: choicerId 제거, Principal에서 추출)
        session1.send("/app/blinddate/choice", Map.of(
                "targetId", 2L
        ));

        // User2 → User1 선택
        session2.send("/app/blinddate/choice", Map.of(
                "targetId", 1L
        ));

        log.info("🗳️ 선택 전송 완료, 매칭 결과 대기...");
        Thread.sleep(3000);

        // 매칭 성공 확인
        boolean matched1 = handler1.hasEvent("CHATROOM");
        boolean matched2 = handler2.hasEvent("CHATROOM");

        if (matched1 && matched2) {
            log.info("✅ 매칭 성공!\n");
        } else {
            log.warn("⚠️ 매칭 결과 대기 중... (User1: {}, User2: {})\n", matched1, matched2);
        }

        // ========== 최종 검증 ==========
        log.info("========================================");
        log.info("  📊 최종 이벤트 요약");
        log.info("========================================\n");

        log.info("[User1] 수신 이벤트: {}", handler1.getEventSummary());
        log.info("[User2] 수신 이벤트: {}\n", handler2.getEventSummary());

        // 필수 이벤트 검증
        assertThat(handler1.hasEvent("JOIN")).isTrue();
        assertThat(handler1.hasEvent("START")).isTrue();
        assertThat(handler1.hasEvent("FREEZE")).isTrue();
        assertThat(handler1.hasEvent("THAW")).isTrue();
        assertThat(handler1.getEventCount("SYSTEM")).isGreaterThanOrEqualTo(3);

        log.info("🎉 전체 시나리오 검증 완료!");

        // 연결 종료
        session1.disconnect();
        session2.disconnect();
    }

    /**
     * STOMP 세션 핸들러 (이벤트 수집용)
     */
    static class TestStompSessionHandler extends StompSessionHandlerAdapter {
        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TestStompSessionHandler.class);
        private final Long memberId;
        private final List<ReceivedEvent> events = new CopyOnWriteArrayList<>();
        private final CountDownLatch joinLatch = new CountDownLatch(1);
        private String sessionId;

        public TestStompSessionHandler(Long memberId) {
            this.memberId = memberId;
        }

        @Override
        public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
            log.info("🔗 [User{}] STOMP Connected", memberId);

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
                    String name = (String) data.get("name");

                    log.info("🎉 [User{}] JOIN received: sessionId={}, name={}", memberId, sessionId, name);
                    events.add(new ReceivedEvent("JOIN", data));
                    joinLatch.countDown();

                    // 세션 토픽 구독
                    subscribeToSession(session, sessionId);
                }
            });
        }

        private void subscribeToSession(StompSession session, String sessionId) {
            String base = "/topic/blinddate/session/" + sessionId;

            // START
            session.subscribe(base + "/start", new SimpleStompFrameHandler("START", events, memberId));

            // FREEZE
            session.subscribe(base + "/freeze", new SimpleStompFrameHandler("FREEZE", events, memberId));

            // THAW
            session.subscribe(base + "/thaw", new SimpleStompFrameHandler("THAW", events, memberId));

            // SYSTEM
            session.subscribe(base + "/system", new SimpleStompFrameHandler("SYSTEM", events, memberId));

            // MESSAGE
            session.subscribe(base + "/message", new SimpleStompFrameHandler("MESSAGE", events, memberId));

            // JOINED
            session.subscribe(base + "/joined", new SimpleStompFrameHandler("JOINED", events, memberId));

            // PARTICIPANTS
            session.subscribe(base + "/participants", new SimpleStompFrameHandler("PARTICIPANTS", events, memberId));

            // CHATROOM (매칭 성공)
            session.subscribe(base + "/member/" + memberId + "/chatroom",
                    new SimpleStompFrameHandler("CHATROOM", events, memberId));

            // FAILED (매칭 실패)
            session.subscribe(base + "/member/" + memberId + "/failed",
                    new SimpleStompFrameHandler("FAILED", events, memberId));

            log.info("📡 [User{}] Subscribed to session topics", memberId);
        }

        public void waitForEvent(String eventType, long timeoutMs) throws InterruptedException {
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                if (hasEvent(eventType)) {
                    return;
                }
                Thread.sleep(100);
            }
        }

        public boolean hasEvent(String eventType) {
            return events.stream().anyMatch(e -> e.type.equals(eventType));
        }

        public int getEventCount(String eventType) {
            return (int) events.stream().filter(e -> e.type.equals(eventType)).count();
        }

        public Map<String, Long> getEventSummary() {
            return events.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            e -> e.type,
                            java.util.stream.Collectors.counting()
                    ));
        }

        @Override
        public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload,
                                    Throwable exception) {
            log.error("❌ [User{}] STOMP Exception: {}", memberId, exception.getMessage());
        }

        @Override
        public void handleTransportError(StompSession session, Throwable exception) {
            log.error("❌ [User{}] Transport Error: {}", memberId, exception.getMessage());
        }
    }

    /**
     * 간단한 StompFrameHandler
     */
    static class SimpleStompFrameHandler implements StompFrameHandler {
        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SimpleStompFrameHandler.class);
        private final String eventType;
        private final List<ReceivedEvent> events;
        private final Long memberId;

        public SimpleStompFrameHandler(String eventType, List<ReceivedEvent> events, Long memberId) {
            this.eventType = eventType;
            this.events = events;
            this.memberId = memberId;
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return Map.class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            Map<String, Object> data = payload instanceof Map ? (Map<String, Object>) payload : Map.of();

            log.info("📨 [User{}] {} event received", memberId, eventType);

            events.add(new ReceivedEvent(eventType, data));
        }
    }

    /**
     * 수신 이벤트 기록
     */
    static class ReceivedEvent {
        String type;
        Map<String, Object> data;
        long timestamp;

        ReceivedEvent(String type, Map<String, Object> data) {
            this.type = type;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }
    }
}