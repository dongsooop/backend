package com.dongsoop.dongsoop.blinddate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.blinddate.dto.StartBlindDateRequest;
import com.dongsoop.dongsoop.blinddate.entity.ParticipantInfo;
import com.dongsoop.dongsoop.blinddate.entity.SessionInfo.SessionState;
import com.dongsoop.dongsoop.blinddate.handler.BlindDateChoiceHandler;
import com.dongsoop.dongsoop.blinddate.handler.BlindDateConnectHandler;
import com.dongsoop.dongsoop.blinddate.handler.BlindDateDisconnectHandler;
import com.dongsoop.dongsoop.blinddate.lock.BlindDateMatchingLock;
import com.dongsoop.dongsoop.blinddate.lock.BlindDateMemberLock;
import com.dongsoop.dongsoop.blinddate.lock.BlindDateSessionLock;
import com.dongsoop.dongsoop.blinddate.notification.BlindDateNotification;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateParticipantStorage;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateParticipantStorageImpl;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateSessionStorage;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateSessionStorageImpl;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateStorage;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateStorageImpl;
import com.dongsoop.dongsoop.blinddate.scheduler.BlindDateSessionScheduler;
import com.dongsoop.dongsoop.blinddate.scheduler.BlindDateTaskScheduler;
import com.dongsoop.dongsoop.blinddate.service.BlindDateServiceImpl;
import com.dongsoop.dongsoop.blinddate.service.BlindDateSessionService;
import com.dongsoop.dongsoop.blinddate.service.BlindDateSessionServiceImpl;
import com.dongsoop.dongsoop.chat.service.ChatRoomService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * 과팅 전체 시나리오 통합 테스트
 * <p>
 * 실제 존재하는 Handler와 Repository를 사용한 통합 테스트
 */
@DisplayName("과팅 전체 시나리오 통합 테스트")
class BlindDateIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(BlindDateIntegrationTest.class);

    private BlindDateServiceImpl blindDateService;
    private BlindDateStorage blindDateStorage;
    private BlindDateSessionStorage sessionStorage;
    private BlindDateParticipantStorage participantStorage;
    private BlindDateSessionService sessionService;
    private BlindDateConnectHandler connectHandler;
    private BlindDateChoiceHandler choiceHandler;
    private BlindDateDisconnectHandler disconnectHandler;
    private BlindDateSessionScheduler sessionScheduler;
    private BlindDateTaskScheduler taskScheduler;
    private BlindDateMatchingLock matchingLock;
    private BlindDateMemberLock memberLock;
    private SimpMessagingTemplate messagingTemplate;
    private BlindDateSessionLock sessionLock;

    @BeforeEach
    void setUp() {
        // Repository 초기화
        blindDateStorage = new BlindDateStorageImpl();
        participantStorage = new BlindDateParticipantStorageImpl();
        sessionStorage = new BlindDateSessionStorageImpl();

        // Lock 초기화
        matchingLock = new BlindDateMatchingLock();
        memberLock = new BlindDateMemberLock();
        sessionLock = new BlindDateSessionLock();

        // Mock 초기화
        messagingTemplate = mock(SimpMessagingTemplate.class);
        BlindDateNotification notification = mock(BlindDateNotification.class);
        sessionScheduler = mock(BlindDateSessionScheduler.class);
        ChatRoomService chatRoomService = mock(ChatRoomService.class);

        // ChatRoomService mock 설정
        when(chatRoomService.createOneToOneChatRoom(anyLong(), anyLong(), anyString()))
                .thenAnswer(invocation -> {
                    Long user1 = invocation.getArgument(0);
                    Long user2 = invocation.getArgument(1);
                    String title = invocation.getArgument(2);
                    return com.dongsoop.dongsoop.chat.entity.ChatRoom.builder()
                            .roomId("chatroom-" + user1 + "-" + user2)
                            .title(title)
                            .build();
                });

        // TaskScheduler 초기화
        taskScheduler = new BlindDateTaskScheduler();

        // Service 초기화
        blindDateService = new BlindDateServiceImpl(
                participantStorage,
                blindDateStorage,
                notification,
                sessionStorage,
                messagingTemplate,
                taskScheduler
        );

        sessionService = new BlindDateSessionServiceImpl(
                participantStorage,
                blindDateStorage
        );

        // Handler 초기화
        connectHandler = new BlindDateConnectHandler(
                participantStorage,
                blindDateStorage,
                sessionStorage,
                blindDateService,
                sessionService,
                sessionScheduler,
                messagingTemplate,
                matchingLock,
                memberLock,
                sessionLock
        );

        choiceHandler = new BlindDateChoiceHandler(
                participantStorage,
                messagingTemplate,
                chatRoomService
        );

        disconnectHandler = new BlindDateDisconnectHandler(
                participantStorage,
                sessionStorage,
                blindDateService,
                matchingLock,
                memberLock,
                sessionLock
        );
    }

    @AfterEach
    void tearDown() {
        // TaskScheduler 정리 - cleanupAllSessions로 변경
        if (taskScheduler != null) {
            taskScheduler.cleanupAllSessions();
        }
    }

    /**
     * 헬퍼 메서드: 세션의 참여자 수 조회
     */
    private int getParticipantCount(String sessionId) {
        return participantStorage.findAllBySessionId(sessionId).size();
    }

    @Nested
    @DisplayName("시나리오 1: 사용자 입장 → 세션 할당")
    class UserEnterScenarios {

        @Test
        @DisplayName("첫 사용자 입장 - Pointer가 없으면 새 세션 생성")
        void firstUser_CreatesNewSession() {
            // given
            blindDateStorage.start(5, LocalDateTime.now().plusHours(1));

            // when
            Map<String, Object> sessionAttributes = new HashMap<>();
            connectHandler.execute("socket-1", 1L, sessionAttributes);
            String sessionId = (String) sessionAttributes.get("sessionId");

            // then
            assertThat(sessionId).isNotNull();
            assertThat(blindDateStorage.getPointer()).isEqualTo(sessionId);
            assertThat(getParticipantCount(sessionId)).isEqualTo(1);

            ParticipantInfo participant = participantStorage.getByMemberId(1L);
            assertThat(participant).isNotNull();
            assertThat(participant.getAnonymousName()).isEqualTo("익명1");
        }

        @Test
        @DisplayName("여러 사용자 입장 - 같은 세션에 배정")
        void multipleUsers_AssignedToSameSession() {
            // given
            blindDateStorage.start(5, LocalDateTime.now().plusHours(1));

            // when
            Map<String, Object> attr1 = new HashMap<>();
            Map<String, Object> attr2 = new HashMap<>();
            Map<String, Object> attr3 = new HashMap<>();

            connectHandler.execute("socket-1", 1L, attr1);
            connectHandler.execute("socket-2", 2L, attr2);
            connectHandler.execute("socket-3", 3L, attr3);

            String session1 = (String) attr1.get("sessionId");
            String session2 = (String) attr2.get("sessionId");
            String session3 = (String) attr3.get("sessionId");

            // then
            assertThat(session1).isEqualTo(session2).isEqualTo(session3);
            assertThat(getParticipantCount(session1)).isEqualTo(3);
        }

        @Test
        @DisplayName("세션 꽉 찬 후 새 사용자 - 새 세션 생성")
        void fullSession_CreatesNewSession() {
            // given
            blindDateStorage.start(3, LocalDateTime.now().plusHours(1));

            // when - 3명 입장 (세션 꽉 참)
            Map<String, Object> attr1 = new HashMap<>();
            connectHandler.execute("socket-1", 1L, attr1);
            connectHandler.execute("socket-2", 2L, attr1);
            connectHandler.execute("socket-3", 3L, attr1);
            String session1 = (String) attr1.get("sessionId");

            // 4번째 사용자
            Map<String, Object> attr2 = new HashMap<>();
            connectHandler.execute("socket-4", 4L, attr2);
            String session2 = (String) attr2.get("sessionId");

            // then
            assertThat(session2).isNotEqualTo(session1);
            assertThat(blindDateStorage.getPointer()).isEqualTo(session2);
        }

        @Test
        @DisplayName("재연결 - 기존 세션으로 복귀, 인원 증가 안 함")
        void reconnect_ReturnsToExistingSession() {
            // given
            blindDateStorage.start(5, LocalDateTime.now().plusHours(1));
            Map<String, Object> attr1 = new HashMap<>();
            connectHandler.execute("socket-1", 1L, attr1);
            String session1 = (String) attr1.get("sessionId");

            // when - 같은 memberId로 재연결
            Map<String, Object> attr2 = new HashMap<>();
            connectHandler.execute("socket-2", 1L, attr2);
            String session2 = (String) attr2.get("sessionId");

            // then
            assertThat(session2).isEqualTo(session1);
            assertThat(getParticipantCount(session1)).isEqualTo(1); // 인원 증가 안 함
        }

        @Test
        @DisplayName("과팅 종료 상태 - 입장 거부")
        void closedBlindDate_RejectsEntry() {
            // given
            blindDateStorage.close();

            // when & then
            Map<String, Object> attr = new HashMap<>();
            assertThatThrownBy(() -> connectHandler.execute("socket-1", 1L, attr))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("과팅이 운영되지 않습니다");
        }
    }

    @Nested
    @DisplayName("시나리오 2: 참여 정보 저장 (세션, 사용자, 소켓)")
    class ParticipantInfoStorage {

        @Test
        @DisplayName("참여 정보 저장 - 세션, 사용자, 소켓 모두 저장")
        void addParticipant_SavesAllInfo() {
            // given
            blindDateStorage.start(5, LocalDateTime.now().plusHours(1));

            // when
            Map<String, Object> attr = new HashMap<>();
            connectHandler.execute("socket-1", 1L, attr);
            String sessionId = (String) attr.get("sessionId");

            // then
            ParticipantInfo byMember = participantStorage.getByMemberId(1L);
            ParticipantInfo bySocket = participantStorage.getBySocketId("socket-1");

            assertThat(byMember).isNotNull();
            assertThat(byMember).isEqualTo(bySocket);
            assertThat(byMember.getSessionId()).isEqualTo(sessionId);
            assertThat(byMember.getMemberId()).isEqualTo(1L);
            assertThat(byMember.getSocketIds()).contains("socket-1");
        }

        @Test
        @DisplayName("익명 이름 순차 할당 - 익명1, 익명2, 익명3...")
        void anonymousNames_AssignedSequentially() {
            // given
            blindDateStorage.start(5, LocalDateTime.now().plusHours(1));

            // when
            Map<String, Object> attr = new HashMap<>();
            connectHandler.execute("socket-1", 1L, attr);
            connectHandler.execute("socket-2", 2L, attr);
            connectHandler.execute("socket-3", 3L, attr);

            // then
            assertThat(participantStorage.getAnonymousName(1L)).isEqualTo("익명1");
            assertThat(participantStorage.getAnonymousName(2L)).isEqualTo("익명2");
            assertThat(participantStorage.getAnonymousName(3L)).isEqualTo("익명3");
        }
    }

    @Nested
    @DisplayName("시나리오 3: 마지막 참여자 → 세션 시작")
    class SessionStartTrigger {

        @Test
        @DisplayName("기준 인원 충족 - 세션 시작 준비")
        void lastParticipant_PrepareToStartSession() throws InterruptedException {
            // given
            blindDateStorage.start(3, LocalDateTime.now().plusHours(1));

            // when - 3명 입장
            Map<String, Object> attr = new HashMap<>();
            connectHandler.execute("socket-1", 1L, attr);
            connectHandler.execute("socket-2", 2L, attr);
            connectHandler.execute("socket-3", 3L, attr);
            String sessionId = (String) attr.get("sessionId");

            // 세션 시작은 비동기이므로 잠시 대기
            Thread.sleep(500);

            // then
            assertThat(getParticipantCount(sessionId)).isEqualTo(3);
            // 세션 시작은 scheduler에 의해 비동기로 처리됨
        }

        @Test
        @DisplayName("기준 인원 미달 - 세션 대기 상태 유지")
        void notFull_RemainsWaiting() throws InterruptedException {
            // given
            blindDateStorage.start(5, LocalDateTime.now().plusHours(1));

            // when - 3명만 입장
            Map<String, Object> attr = new HashMap<>();
            connectHandler.execute("socket-1", 1L, attr);
            connectHandler.execute("socket-2", 2L, attr);
            connectHandler.execute("socket-3", 3L, attr);
            String sessionId = (String) attr.get("sessionId");

            Thread.sleep(500);

            // then
            SessionState state = sessionStorage.getState(sessionId);
            assertThat(state).isEqualTo(SessionState.WAITING);
        }
    }

    @Nested
    @DisplayName("시나리오 4: 퇴장 처리")
    class LeaveScenarios {

        @Test
        @DisplayName("소켓 연결 해제 - 참여 정보 제거")
        void disconnect_RemovesSocket() {
            // given
            blindDateStorage.start(5, LocalDateTime.now().plusHours(1));
            Map<String, Object> attr = new HashMap<>();
            connectHandler.execute("socket-1", 1L, attr);
            connectHandler.execute("socket-2", 2L, attr);
            String sessionId = (String) attr.get("sessionId");

            assertThat(getParticipantCount(sessionId)).isEqualTo(2);

            // when
            disconnectHandler.execute("socket-1", 1L, sessionId);

            // then
            assertThat(participantStorage.getByMemberId(1L)).isNull();
        }
    }

    @Nested
    @DisplayName("시나리오 5: 동시성 테스트")
    class ConcurrencyScenarios {

        @Test
        @DisplayName("100명 동시 입장 - 정확한 세션 분배")
        void concurrent100Users_AccurateDistribution() throws Exception {
            // given
            blindDateStorage.start(10, LocalDateTime.now().plusHours(1));

            ExecutorService executor = Executors.newFixedThreadPool(20);
            CountDownLatch latch = new CountDownLatch(100);
            Set<String> sessions = java.util.concurrent.ConcurrentHashMap.newKeySet();
            List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

            // when - 100명 동시 입장
            for (int i = 1; i <= 100; i++) {
                final long memberId = i;
                executor.submit(() -> {
                    try {
                        Map<String, Object> attr = new HashMap<>();
                        connectHandler.execute("socket-" + memberId, memberId, attr);
                        String sessionId = (String) attr.get("sessionId");
                        if (sessionId != null) {
                            sessions.add(sessionId);
                        }
                    } catch (Exception e) {
                        exceptions.add(e);
                        log.error("Error during concurrent enter for memberId={}", memberId, e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertThat(latch.await(30, TimeUnit.SECONDS)).as("모든 스레드가 완료되어야 함").isTrue();
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);

            if (!exceptions.isEmpty()) {
                log.error("=== Exceptions in 100 users test ===");
                exceptions.forEach(e -> log.error("Exception: {}", e.getMessage(), e));
            }

            // 예외 없어야 함
            assertThat(exceptions).as("예외 없어야 함").isEmpty();

            log.info("Created {} sessions for 100 users", sessions.size());
            assertThat(sessions).as("최소 10개 세션 생성되어야 함").hasSizeGreaterThanOrEqualTo(10);

            // 모든 사용자가 세션에 배정되었는지
            for (int i = 1; i <= 100; i++) {
                ParticipantInfo participant = participantStorage.getByMemberId((long) i);
                assertThat(participant).as("Member " + i + "가 배정되어야 함").isNotNull();
            }
        }

        @Test
        @DisplayName("동시 입장 - Pointer 동기화 보장")
        void concurrentEnter_PointerSynchronized() throws Exception {
            // given
            blindDateStorage.start(5, LocalDateTime.now().plusHours(1));

            ExecutorService executor = Executors.newFixedThreadPool(10);
            CountDownLatch latch = new CountDownLatch(15);
            List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

            // when - 15명 동시 입장
            for (int i = 1; i <= 15; i++) {
                final long memberId = i;
                executor.submit(() -> {
                    try {
                        Map<String, Object> attr = new HashMap<>();
                        connectHandler.execute("socket-" + memberId, memberId, attr);
                    } catch (Exception e) {
                        exceptions.add(e);
                        log.error("Error for memberId={}", memberId, e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertThat(latch.await(15, TimeUnit.SECONDS)).as("모든 스레드가 완료되어야 함").isTrue();
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);

            if (!exceptions.isEmpty()) {
                log.error("=== Exceptions in pointer sync test ===");
                exceptions.forEach(e -> log.error("Exception: {}", e.getMessage(), e));
            }

            // then - 3개 세션으로 정확히 분배 (15 / 5 = 3)
            assertThat(exceptions).as("예외 없어야 함").isEmpty();

            Set<String> uniqueSessions = new java.util.HashSet<>();
            for (int i = 1; i <= 15; i++) {
                ParticipantInfo p = participantStorage.getByMemberId((long) i);
                assertThat(p).as("Member " + i + "가 배정되어야 함").isNotNull();
                uniqueSessions.add(p.getSessionId());
            }

            log.info("Created {} sessions for 15 users with capacity 5", uniqueSessions.size());

            // PointerLock 적용 후: 정확히 3개 세션만 생성되어야 함
            assertThat(uniqueSessions).as("정확히 3개 세션이 생성되어야 함 (15명 / 5명)").hasSize(3);

            // 각 세션의 인원 확인
            for (String sessionId : uniqueSessions) {
                int count = participantStorage.findAllBySessionId(sessionId).size();
                log.info("Session {} has {} participants", sessionId, count);

                // 정원 절대 초과 불가!
                assertThat(count).as("각 세션은 정원을 절대 초과하면 안 됨").isLessThanOrEqualTo(5);
            }

            // 총 인원 확인 - 15명 모두 배정되어야 함
            int totalCount = uniqueSessions.stream()
                    .mapToInt(sid -> participantStorage.findAllBySessionId(sid).size())
                    .sum();
            assertThat(totalCount).as("총 15명이 배정되어야 함").isEqualTo(15);
        }
    }

    @Nested
    @DisplayName("시나리오 6: 사랑의 작대기 - 매칭")
    class MatchingScenarios {

        @Test
        @DisplayName("서로 선택 - 매칭 성공")
        void mutualChoice_Matches() {
            // given
            blindDateStorage.start(2, LocalDateTime.now().plusHours(1));
            Map<String, Object> attr = new HashMap<>();
            connectHandler.execute("socket-1", 1L, attr);
            connectHandler.execute("socket-2", 2L, attr);
            String sessionId = (String) attr.get("sessionId");

            // when
            choiceHandler.execute(sessionId, 1L, 2L);
            choiceHandler.execute(sessionId, 2L, 1L);

            // then
            Set<Long> notMatched = participantStorage.getNotMatched(sessionId);
            assertThat(notMatched).isEmpty();

            // 매칭 확인
            assertThat(participantStorage.isMatched(sessionId, 1L)).isTrue();
            assertThat(participantStorage.isMatched(sessionId, 2L)).isTrue();
        }

        @Test
        @DisplayName("한쪽만 선택 - 매칭 실패")
        void oneWayChoice_NoMatch() {
            // given
            blindDateStorage.start(2, LocalDateTime.now().plusHours(1));
            Map<String, Object> attr = new HashMap<>();
            connectHandler.execute("socket-1", 1L, attr);
            connectHandler.execute("socket-2", 2L, attr);
            String sessionId = (String) attr.get("sessionId");

            // when
            choiceHandler.execute(sessionId, 1L, 2L);

            // then
            Set<Long> notMatched = participantStorage.getNotMatched(sessionId);
            assertThat(notMatched).containsExactlyInAnyOrder(1L, 2L);

            assertThat(participantStorage.isMatched(sessionId, 1L)).isFalse();
            assertThat(participantStorage.isMatched(sessionId, 2L)).isFalse();
        }

        @Test
        @DisplayName("삼각관계 - 모두 매칭 실패")
        void triangleChoice_AllFail() {
            // given
            blindDateStorage.start(3, LocalDateTime.now().plusHours(1));
            Map<String, Object> attr = new HashMap<>();
            connectHandler.execute("socket-1", 1L, attr);
            connectHandler.execute("socket-2", 2L, attr);
            connectHandler.execute("socket-3", 3L, attr);
            String sessionId = (String) attr.get("sessionId");

            // when - 1→2, 2→3, 3→1
            choiceHandler.execute(sessionId, 1L, 2L);
            choiceHandler.execute(sessionId, 2L, 3L);
            choiceHandler.execute(sessionId, 3L, 1L);

            // then
            Set<Long> notMatched = participantStorage.getNotMatched(sessionId);
            assertThat(notMatched).containsExactlyInAnyOrder(1L, 2L, 3L);
        }

        @Test
        @DisplayName("5명 중 2쌍 매칭, 1명 실패")
        void partialMatching() {
            // given
            blindDateStorage.start(5, LocalDateTime.now().plusHours(1));
            Map<String, Object> attr = new HashMap<>();
            connectHandler.execute("socket-1", 1L, attr);
            for (long i = 2; i <= 5; i++) {
                connectHandler.execute("socket-" + i, i, attr);
            }
            String sessionId = (String) attr.get("sessionId");

            // when - 1↔2, 3↔4, 5 혼자
            choiceHandler.execute(sessionId, 1L, 2L);
            choiceHandler.execute(sessionId, 2L, 1L);
            choiceHandler.execute(sessionId, 3L, 4L);
            choiceHandler.execute(sessionId, 4L, 3L);
            choiceHandler.execute(sessionId, 5L, 1L);

            // then
            Set<Long> notMatched = participantStorage.getNotMatched(sessionId);
            assertThat(notMatched).containsExactly(5L);
        }
    }

    @Nested
    @DisplayName("시나리오 7: 전체 플로우")
    class CompleteFlow {

        @Test
        @DisplayName("전체 플로우: 시작 → 입장 → 매칭")
        void completeFlow_StartToEnd() throws InterruptedException {
            // 1. 과팅 시작
            LocalDateTime expiredDate = LocalDateTime.now().plusHours(1);
            StartBlindDateRequest request = new StartBlindDateRequest(expiredDate, 3);
            blindDateService.startBlindDate(request);

            assertThat(blindDateService.isAvailable()).isTrue();

            // 2. 3명 입장
            Map<String, Object> attr = new HashMap<>();
            connectHandler.execute("socket-1", 1L, attr);
            connectHandler.execute("socket-2", 2L, attr);
            connectHandler.execute("socket-3", 3L, attr);
            String sessionId = (String) attr.get("sessionId");

            Thread.sleep(500);

            // 3. 사랑의 작대기 - 1↔2 매칭
            choiceHandler.execute(sessionId, 1L, 2L);
            choiceHandler.execute(sessionId, 2L, 1L);

            // 4. 매칭 결과 확인
            Set<Long> notMatched = participantStorage.getNotMatched(sessionId);
            assertThat(notMatched).containsExactly(3L);

            assertThat(participantStorage.isMatched(sessionId, 1L)).isTrue();
            assertThat(participantStorage.isMatched(sessionId, 2L)).isTrue();
            assertThat(participantStorage.isMatched(sessionId, 3L)).isFalse();
        }
    }
}
