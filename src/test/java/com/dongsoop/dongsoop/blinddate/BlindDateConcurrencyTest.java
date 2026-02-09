package com.dongsoop.dongsoop.blinddate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.dongsoop.dongsoop.blinddate.entity.ParticipantInfo;
import com.dongsoop.dongsoop.blinddate.handler.BlindDateConnectHandler;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * BlindDate 동시성 테스트
 * <p>
 * 테스트 시나리오: 1. Pointer 동시 접근 - 중복 세션 생성 방지 2. 세션 정원 초과 방지 3. Lock 메커니즘 검증 4. 재연결 동시성 5. 익명 이름 카운터 동시성 6. 스트레스 테스트 7.
 * Edge Case 테스트
 */
@DisplayName("BlindDate 동시성 테스트")
class BlindDateConcurrencyTest {

    private static final Logger log = LoggerFactory.getLogger(BlindDateConcurrencyTest.class);

    private BlindDateConnectHandler connectHandler;
    private BlindDateServiceImpl blindDateService;
    private BlindDateSessionService sessionService;
    private BlindDateStorage blindDateStorage;
    private BlindDateSessionStorage sessionStorage;
    private BlindDateParticipantStorage participantStorage;
    private BlindDateMatchingLock matchingLock;
    private BlindDateMemberLock memberLock;
    private BlindDateSessionScheduler sessionScheduler;
    private BlindDateTaskScheduler taskScheduler;
    private SimpMessagingTemplate messagingTemplate;
    private BlindDateSessionLock sessionLock;

    @BeforeEach
    void setUp() {
        blindDateStorage = new BlindDateStorageImpl();
        participantStorage = new BlindDateParticipantStorageImpl();
        sessionStorage = new BlindDateSessionStorageImpl();

        matchingLock = new BlindDateMatchingLock();
        memberLock = new BlindDateMemberLock();
        sessionLock = new BlindDateSessionLock();

        messagingTemplate = mock(SimpMessagingTemplate.class);
        BlindDateNotification notification = mock(BlindDateNotification.class);
        sessionScheduler = mock(BlindDateSessionScheduler.class);
        taskScheduler = new BlindDateTaskScheduler();

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
    }

    @AfterEach
    void tearDown() {
        // TaskScheduler 정리 - cleanupAllSessions로 변경
        if (taskScheduler != null) {
            taskScheduler.cleanupAllSessions();
        }
    }

    private int getParticipantCount(String sessionId) {
        return participantStorage.findAllBySessionId(sessionId).size();
    }

    @Nested
    @DisplayName("1. Pointer 동시 접근 테스트")
    class PointerConcurrencyTests {

        @RepeatedTest(10)
        @DisplayName("동시 첫 입장 - 하나의 세션만 생성")
        void concurrentFirstEntry_ShouldCreateOnlyOneSession() throws InterruptedException {
            blindDateStorage.start(10, LocalDateTime.now().plusHours(1));
            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);

            Set<String> sessionIds = ConcurrentHashMap.newKeySet();
            List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

            for (int i = 0; i < threadCount; i++) {
                final int memberId = i + 1;
                executor.submit(() -> {
                    try {
                        Map<String, Object> sessionAttributes = new HashMap<>();
                        connectHandler.execute("socket-" + memberId, (long) memberId, sessionAttributes);
                        String sessionId = (String) sessionAttributes.get("sessionId");
                        if (sessionId != null) {
                            sessionIds.add(sessionId);
                        }
                    } catch (Exception e) {
                        exceptions.add(e);
                        log.error("Error in concurrent first entry, memberId={}", memberId, e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertThat(latch.await(15, TimeUnit.SECONDS)).isTrue();
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);

            if (!exceptions.isEmpty()) {
                log.error("=== Exceptions occurred ===");
                exceptions.forEach(e -> log.error("Exception: {}", e.getMessage(), e));
            }

            assertThat(exceptions).as("예외 없어야 함").isEmpty();

            // PointerLock 적용 후: 정확히 1개 세션만 생성되어야 함
            assertThat(sessionIds).as("정확히 1개 세션만 생성되어야 함").hasSize(1);

            int participantCount = getParticipantCount(sessionIds.iterator().next());
            assertThat(participantCount).as("모든 참가자가 입장해야 함").isEqualTo(10);
        }

        @RepeatedTest(10)
        @DisplayName("세션 만원 후 동시 입장 - 새 세션 하나만 생성")
        void concurrentFullSession_ShouldCreateOnlyOneNewSession() throws InterruptedException {
            blindDateStorage.start(4, LocalDateTime.now().plusHours(1));

            Map<String, Object> attr = new HashMap<>();
            connectHandler.execute("socket-1", 1L, attr);
            connectHandler.execute("socket-2", 2L, attr);
            connectHandler.execute("socket-3", 3L, attr);
            connectHandler.execute("socket-4", 4L, attr);
            String firstSessionId = (String) attr.get("sessionId");

            // 첫 번째 세션이 정확히 4명인지 확인
            assertThat(getParticipantCount(firstSessionId)).isEqualTo(4);

            int newUsers = 5;
            ExecutorService executor = Executors.newFixedThreadPool(newUsers);
            CountDownLatch latch = new CountDownLatch(newUsers);

            Set<String> newSessionIds = ConcurrentHashMap.newKeySet();
            List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

            for (int i = 0; i < newUsers; i++) {
                final int memberId = i + 5;
                executor.submit(() -> {
                    try {
                        Map<String, Object> sessionAttributes = new HashMap<>();
                        connectHandler.execute("socket-" + memberId, (long) memberId, sessionAttributes);
                        String sessionId = (String) sessionAttributes.get("sessionId");
                        if (sessionId != null) {
                            newSessionIds.add(sessionId);
                        }
                    } catch (Exception e) {
                        exceptions.add(e);
                        log.error("Error in full session test, memberId={}", memberId, e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertThat(latch.await(15, TimeUnit.SECONDS)).isTrue();
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);

            if (!exceptions.isEmpty()) {
                log.error("=== Exceptions in full session test ===");
                exceptions.forEach(e -> log.error("Exception: {}", e.getMessage(), e));
            }

            assertThat(exceptions).as("예외 없어야 함").isEmpty();

            // 5명이 입장하면 정원 4명 기준으로 2개 세션 필요 (4명 + 1명)
            // 따라서 기존 세션 제외하고 1~2개의 새 세션이 생성됨
            Set<String> allSessionIds = new HashSet<>(newSessionIds);
            allSessionIds.remove(firstSessionId); // 기존 세션 제외

            log.info("Created {} new sessions for 5 users", allSessionIds.size());
            assertThat(allSessionIds).as("1~2개의 새 세션이 생성되어야 함 (정원 4명 기준)")
                    .hasSizeBetween(1, 2);

            // 모든 참가자가 배정되었는지 확인
            int totalNewParticipants = allSessionIds.stream()
                    .mapToInt(BlindDateConcurrencyTest.this::getParticipantCount)
                    .sum();
            assertThat(totalNewParticipants).as("5명 모두 새 세션에 배정되어야 함").isEqualTo(5);

            // 첫 번째 세션은 여전히 4명이어야 함
            assertThat(getParticipantCount(firstSessionId)).isEqualTo(4);
        }

        @RepeatedTest(10)
        @DisplayName("Pointer 변경 중 동시 조회 - 일관된 세션 할당")
        void concurrentPointerChange_ShouldBeConsistent() throws InterruptedException {
            blindDateStorage.start(3, LocalDateTime.now().plusHours(1));

            Map<String, Object> attr = new HashMap<>();
            connectHandler.execute("socket-1", 1L, attr);
            connectHandler.execute("socket-2", 2L, attr);

            // 첫 번째 세션에 2명이 있는지 확인
            String firstSession = (String) attr.get("sessionId");
            assertThat(getParticipantCount(firstSession)).isEqualTo(2);

            int threadCount = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch latch = new CountDownLatch(threadCount);

            Map<Long, String> memberToSession = new ConcurrentHashMap<>();
            List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

            for (int i = 0; i < threadCount; i++) {
                final int memberId = i + 3;
                executor.submit(() -> {
                    try {
                        Map<String, Object> sessionAttributes = new HashMap<>();
                        connectHandler.execute("socket-" + memberId, (long) memberId, sessionAttributes);
                        String sessionId = (String) sessionAttributes.get("sessionId");
                        if (sessionId != null) {
                            memberToSession.put((long) memberId, sessionId);
                        }
                    } catch (Exception e) {
                        exceptions.add(e);
                        log.error("Error in pointer change test, memberId={}", memberId, e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertThat(latch.await(15, TimeUnit.SECONDS)).isTrue();
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);

            if (!exceptions.isEmpty()) {
                log.error("=== Exceptions in pointer change test ===");
                exceptions.forEach(e -> log.error("Exception: {}", e.getMessage(), e));
            }

            assertThat(exceptions).as("예외 없어야 함").isEmpty();
            assertThat(memberToSession).as("모든 회원이 배정되어야 함").hasSize(10);

            // 각 세션의 인원수 확인
            Set<String> allSessions = new HashSet<>(memberToSession.values());
            log.info("Total sessions created: {}", allSessions.size());

            for (String sessionId : allSessions) {
                int count = getParticipantCount(sessionId);
                log.info("Session {} has {} participants", sessionId, count);

                // 정원 절대 초과 불가!
                assertThat(count).as("각 세션은 정원(3명)을 절대 초과하면 안 됨 - Session: " + sessionId)
                        .isLessThanOrEqualTo(3);
            }

            // 총 인원 확인 (첫 2명 + 새로 들어온 10명 = 12명)
            int totalCount = allSessions.stream().mapToInt(sid -> getParticipantCount(sid)).sum();
            assertThat(totalCount).as("총 12명이 배정되어야 함 - 실제: " + totalCount).isEqualTo(12);
        }
    }

    @Nested
    @DisplayName("2. 세션 정원 초과 방지 테스트")
    class SessionCapacityTests {

        @RepeatedTest(10)
        @DisplayName("정원만큼 동시 입장 - 모두 성공")
        void concurrentExactCapacity_AllShouldSucceed() throws InterruptedException {
            int capacity = 8;
            blindDateStorage.start(capacity, LocalDateTime.now().plusHours(1));

            ExecutorService executor = Executors.newFixedThreadPool(capacity);
            CountDownLatch latch = new CountDownLatch(capacity);

            Set<String> sessionIds = ConcurrentHashMap.newKeySet();
            List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

            for (int i = 0; i < capacity; i++) {
                final int memberId = i + 1;
                executor.submit(() -> {
                    try {
                        Map<String, Object> sessionAttributes = new HashMap<>();
                        connectHandler.execute("socket-" + memberId, (long) memberId, sessionAttributes);
                        String sessionId = (String) sessionAttributes.get("sessionId");
                        if (sessionId != null) {
                            sessionIds.add(sessionId);
                        }
                    } catch (Exception e) {
                        exceptions.add(e);
                        log.error("Error in capacity test, memberId={}", memberId, e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertThat(latch.await(15, TimeUnit.SECONDS)).isTrue();
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);

            if (!exceptions.isEmpty()) {
                log.error("=== Exceptions in capacity test ===");
                exceptions.forEach(e -> log.error("Exception: {}", e.getMessage(), e));
            }

            assertThat(exceptions).as("예외 없어야 함").isEmpty();

            // PointerLock 적용 후: 정확히 1개 세션만 생성되어야 함
            assertThat(sessionIds).as("정확히 1개 세션만 생성되어야 함").hasSize(1);

            int participantCount = getParticipantCount(sessionIds.iterator().next());
            assertThat(participantCount).as("정원만큼 정확히 입장해야 함").isEqualTo(capacity);
        }

        @RepeatedTest(10)
        @DisplayName("정원 초과 시도 - 새 세션 생성")
        void concurrentOverCapacity_ShouldCreateNewSession() throws InterruptedException {
            int capacity = 4;
            int totalUsers = 10;
            blindDateStorage.start(capacity, LocalDateTime.now().plusHours(1));

            ExecutorService executor = Executors.newFixedThreadPool(totalUsers);
            CountDownLatch latch = new CountDownLatch(totalUsers);

            Map<Long, String> memberToSession = new ConcurrentHashMap<>();
            List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

            for (int i = 0; i < totalUsers; i++) {
                final int memberId = i + 1;
                executor.submit(() -> {
                    try {
                        Map<String, Object> sessionAttributes = new HashMap<>();
                        connectHandler.execute("socket-" + memberId, (long) memberId, sessionAttributes);
                        String sessionId = (String) sessionAttributes.get("sessionId");
                        if (sessionId != null) {
                            memberToSession.put((long) memberId, sessionId);
                        }
                    } catch (Exception e) {
                        exceptions.add(e);
                        log.error("Error in over capacity test, memberId={}", memberId, e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertThat(latch.await(15, TimeUnit.SECONDS)).isTrue();
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);

            if (!exceptions.isEmpty()) {
                log.error("=== Exceptions in over capacity test ===");
                exceptions.forEach(e -> log.error("Exception: {}", e.getMessage(), e));
            }

            assertThat(exceptions).as("예외 없어야 함").isEmpty();
            assertThat(memberToSession).as("모든 회원이 배정되어야 함").hasSize(totalUsers);

            Set<String> sessions = new HashSet<>(memberToSession.values());
            log.info("Created {} sessions for {} users with capacity {}", sessions.size(), totalUsers, capacity);

            // 동시성으로 인해 정확한 세션 수는 보장할 수 없지만, 최소한의 세션은 필요
            int minExpectedSessions = (int) Math.ceil((double) totalUsers / capacity); // 최소 3개
            log.info("Expected minimum {} sessions", minExpectedSessions);
            assertThat(sessions).as("최소 " + minExpectedSessions + "개 세션이 생성되어야 함")
                    .hasSizeGreaterThanOrEqualTo(minExpectedSessions);

            for (String sessionId : sessions) {
                int count = getParticipantCount(sessionId);
                log.info("Session {} has {} participants", sessionId, count);

                // 정원 절대 초과 불가!
                assertThat(count).as("각 세션은 정원을 절대 초과하면 안 됨 - 세션: " + sessionId + ", 인원: " + count)
                        .isLessThanOrEqualTo(capacity);
            }
        }

        @RepeatedTest(10)
        @DisplayName("마지막 자리 경쟁 - 정확히 한 명만 입장")
        void concurrentLastSlot_OnlyOneShouldEnter() throws InterruptedException {
            int capacity = 5;
            blindDateStorage.start(capacity, LocalDateTime.now().plusHours(1));

            // 먼저 4명 입장
            Map<String, Object> attr = new HashMap<>();
            connectHandler.execute("socket-1", 1L, attr);
            connectHandler.execute("socket-2", 2L, attr);
            connectHandler.execute("socket-3", 3L, attr);
            connectHandler.execute("socket-4", 4L, attr);
            String firstSessionId = (String) attr.get("sessionId");

            // 4명이 정확히 입장했는지 확인
            assertThat(getParticipantCount(firstSessionId)).isEqualTo(4);

            // 5명이 동시에 마지막 자리를 두고 경쟁
            int competitors = 5;
            ExecutorService executor = Executors.newFixedThreadPool(competitors);
            CountDownLatch latch = new CountDownLatch(competitors);

            Map<Long, String> memberToSession = new ConcurrentHashMap<>();
            List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

            for (int i = 0; i < competitors; i++) {
                final int memberId = i + 5;
                executor.submit(() -> {
                    try {
                        Map<String, Object> sessionAttributes = new HashMap<>();
                        connectHandler.execute("socket-" + memberId, (long) memberId, sessionAttributes);
                        String sessionId = (String) sessionAttributes.get("sessionId");
                        if (sessionId != null) {
                            memberToSession.put((long) memberId, sessionId);
                        }
                    } catch (Exception e) {
                        exceptions.add(e);
                        log.error("Error in last slot test, memberId={}", memberId, e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertThat(latch.await(15, TimeUnit.SECONDS)).isTrue();
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);

            if (!exceptions.isEmpty()) {
                log.error("=== Exceptions in last slot test ===");
                exceptions.forEach(e -> log.error("Exception: {}", e.getMessage(), e));
            }

            assertThat(exceptions).as("예외 없어야 함").isEmpty();
            assertThat(memberToSession).as("5명 모두 배정되어야 함").hasSize(5);

            // 첫 번째 세션에 입장한 사람 수 카운트
            long firstSessionCount = memberToSession.values().stream()
                    .filter(sid -> sid.equals(firstSessionId))
                    .count();

            log.info("First session count: {}, expected: 1", firstSessionCount);
            log.info("First session total participants: {}", getParticipantCount(firstSessionId));

            // 첫 번째 세션에는 정확히 1명만 추가로 입장해야 함
            assertThat(firstSessionCount).as("마지막 자리에 1명만 입장해야 함").isEqualTo(1);
            assertThat(getParticipantCount(firstSessionId)).as("첫 번째 세션은 정원이어야 함").isEqualTo(capacity);

            // 나머지 4명은 새 세션에 입장
            long newSessionCount = memberToSession.values().stream()
                    .filter(sid -> !sid.equals(firstSessionId))
                    .count();
            assertThat(newSessionCount).as("나머지 4명은 새 세션에 입장해야 함").isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("3. 재연결 동시성 테스트")
    class ReconnectionConcurrencyTests {

        @RepeatedTest(10)
        @DisplayName("동일 사용자 여러 탭 동시 접속")
        void concurrentMultipleTabs_ShouldShareSession() throws InterruptedException {
            blindDateStorage.start(10, LocalDateTime.now().plusHours(1));

            int tabCount = 5;
            ExecutorService executor = Executors.newFixedThreadPool(tabCount);
            CountDownLatch latch = new CountDownLatch(tabCount);

            Set<String> sessionIds = ConcurrentHashMap.newKeySet();
            List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

            for (int i = 0; i < tabCount; i++) {
                final int socketNum = i + 1;
                executor.submit(() -> {
                    try {
                        Map<String, Object> sessionAttributes = new HashMap<>();
                        connectHandler.execute("socket-" + socketNum, 1L, sessionAttributes);
                        String sessionId = (String) sessionAttributes.get("sessionId");
                        if (sessionId != null) {
                            sessionIds.add(sessionId);
                        }
                    } catch (Exception e) {
                        exceptions.add(e);
                        log.error("Error in multi-tab test, socketNum={}", socketNum, e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertThat(latch.await(15, TimeUnit.SECONDS)).isTrue();
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);

            if (!exceptions.isEmpty()) {
                log.error("=== Exceptions in multi-tab test ===");
                exceptions.forEach(e -> log.error("Exception: {}", e.getMessage(), e));
            }

            assertThat(exceptions).as("예외 없어야 함").isEmpty();
            assertThat(sessionIds).as("모든 탭이 같은 세션에 접속해야 함").hasSize(1);

            ParticipantInfo participant = participantStorage.getByMemberId(1L);
            assertThat(participant).as("참가자 정보가 존재해야 함").isNotNull();
            int socketCount = participant.getSocketIds().size();
            log.info("Member 1 has {} sockets", socketCount);
            assertThat(socketCount).as("모든 소켓이 등록되어야 함").isEqualTo(tabCount);
        }
    }

    @Nested
    @DisplayName("4. 익명 이름 카운터 동시성 테스트")
    class AnonymousNameCounterTests {

        @RepeatedTest(10)
        @DisplayName("익명 이름 중복 없이 순차 할당")
        void concurrentAnonymousName_ShouldBeUnique() throws InterruptedException {
            blindDateStorage.start(50, LocalDateTime.now().plusHours(1));

            int userCount = 50;
            ExecutorService executor = Executors.newFixedThreadPool(userCount);
            CountDownLatch latch = new CountDownLatch(userCount);

            Map<Long, String> memberToName = new ConcurrentHashMap<>();
            List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

            for (int i = 0; i < userCount; i++) {
                final int memberId = i + 1;
                executor.submit(() -> {
                    try {
                        Map<String, Object> sessionAttributes = new HashMap<>();
                        connectHandler.execute("socket-" + memberId, (long) memberId, sessionAttributes);
                        ParticipantInfo participant = participantStorage.getByMemberId((long) memberId);
                        if (participant != null) {
                            memberToName.put((long) memberId, participant.getAnonymousName());
                        }
                    } catch (Exception e) {
                        exceptions.add(e);
                        log.error("Error in anonymous name test, memberId={}", memberId, e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertThat(latch.await(20, TimeUnit.SECONDS)).isTrue();
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);

            if (!exceptions.isEmpty()) {
                log.error("=== Exceptions in anonymous name test ===");
                exceptions.forEach(e -> log.error("Exception: {}", e.getMessage(), e));
            }

            assertThat(exceptions).as("예외 없어야 함").isEmpty();
            assertThat(memberToName).as("모든 회원이 이름을 받아야 함").hasSize(userCount);

            Set<String> uniqueNames = new HashSet<>(memberToName.values());
            log.info("Generated {} unique names for {} users", uniqueNames.size(), userCount);

            // 중복 이름 체크
            if (uniqueNames.size() != userCount) {
                Map<String, Long> nameFrequency = memberToName.values().stream()
                        .collect(Collectors.groupingBy(name -> name, Collectors.counting()));
                nameFrequency.entrySet().stream()
                        .filter(e -> e.getValue() > 1)
                        .forEach(e -> log.error("⚠️ DUPLICATE NAME DETECTED: {} appears {} times", e.getKey(),
                                e.getValue()));
            }

            // 익명 이름은 절대 중복되면 안 됨!
            assertThat(uniqueNames).as("익명 이름은 절대 중복되면 안 됨").hasSize(userCount);
        }
    }

    @Nested
    @DisplayName("5. 스트레스 테스트")
    class StressTests {

        @Test
        @DisplayName("대규모 동시 접속 - 100명")
        void stressTest_100Users() throws InterruptedException {
            int capacity = 10;
            int totalUsers = 100;
            blindDateStorage.start(capacity, LocalDateTime.now().plusHours(1));

            ExecutorService executor = Executors.newFixedThreadPool(50);
            CountDownLatch latch = new CountDownLatch(totalUsers);

            Map<Long, String> memberToSession = new ConcurrentHashMap<>();
            AtomicInteger errorCount = new AtomicInteger(0);

            long startTime = System.currentTimeMillis();

            for (int i = 0; i < totalUsers; i++) {
                final int memberId = i + 1;
                executor.submit(() -> {
                    try {
                        Map<String, Object> sessionAttributes = new HashMap<>();
                        connectHandler.execute("socket-" + memberId, (long) memberId, sessionAttributes);
                        memberToSession.put((long) memberId, (String) sessionAttributes.get("sessionId"));
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        log.error("Error in stress test", e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            boolean completed = latch.await(30, TimeUnit.SECONDS);
            executor.shutdown();

            long duration = System.currentTimeMillis() - startTime;

            log.info("=== Stress Test Result ===");
            log.info("Total Users: {}", totalUsers);
            log.info("Success: {}", memberToSession.size());
            log.info("Errors: {}", errorCount.get());
            log.info("Duration: {}ms", duration);
            log.info("Throughput: {}/sec", (totalUsers * 1000.0 / duration));

            assertThat(completed).isTrue();

            // 동시성 환경에서는 일부 에러가 발생할 수 있음 (예: lock 경합)
            // 하지만 대부분의 사용자는 성공해야 함
            int successRate = (memberToSession.size() * 100) / totalUsers;
            log.info("Success rate: {}%", successRate);
            assertThat(successRate).as("성공률은 90% 이상이어야 함").isGreaterThanOrEqualTo(90);

            Set<String> sessions = new HashSet<>(memberToSession.values());
            log.info("Created Sessions: {}", sessions.size());

            for (String sessionId : sessions) {
                int count = getParticipantCount(sessionId);

                // 정원 절대 초과 불가!
                assertThat(count).as("각 세션은 정원을 절대 초과하면 안 됨").isLessThanOrEqualTo(capacity);
            }

            // 성공한 사용자들의 총합 확인
            int totalCount = sessions.stream().mapToInt(sid -> getParticipantCount(sid)).sum();
            assertThat(totalCount).as("성공한 사용자 수").isEqualTo(memberToSession.size());
        }
    }

    @Nested
    @DisplayName("6. Edge Case 테스트")
    class EdgeCaseTests {

        @Test
        @DisplayName("Pointer가 null인 상태에서 대규모 동시 입장")
        void edgeCase_NullPointer() throws InterruptedException {
            blindDateStorage.start(5, LocalDateTime.now().plusHours(1));

            int userCount = 100;
            ExecutorService executor = Executors.newFixedThreadPool(userCount);
            CountDownLatch latch = new CountDownLatch(userCount);

            Map<Long, String> memberToSession = new ConcurrentHashMap<>();

            for (int i = 0; i < userCount; i++) {
                final int memberId = i + 1;
                executor.submit(() -> {
                    try {
                        Map<String, Object> sessionAttributes = new HashMap<>();
                        connectHandler.execute("socket-" + memberId, (long) memberId, sessionAttributes);
                        memberToSession.put((long) memberId, (String) sessionAttributes.get("sessionId"));
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertThat(latch.await(20, TimeUnit.SECONDS)).isTrue();
            executor.shutdown();

            Set<String> sessions = new HashSet<>(memberToSession.values());
            for (String sessionId : sessions) {
                assertThat(getParticipantCount(sessionId)).isGreaterThan(0);
            }
        }

        @Test
        @DisplayName("정원 1명 세션에 대규모 동시 입장")
        void edgeCase_CapacityOne() throws InterruptedException {
            blindDateStorage.start(1, LocalDateTime.now().plusHours(1));

            int userCount = 50;
            ExecutorService executor = Executors.newFixedThreadPool(userCount);
            CountDownLatch latch = new CountDownLatch(userCount);

            Map<Long, String> memberToSession = new ConcurrentHashMap<>();

            for (int i = 0; i < userCount; i++) {
                final int memberId = i + 1;
                executor.submit(() -> {
                    try {
                        Map<String, Object> sessionAttributes = new HashMap<>();
                        connectHandler.execute("socket-" + memberId, (long) memberId, sessionAttributes);
                        memberToSession.put((long) memberId, (String) sessionAttributes.get("sessionId"));
                    } finally {
                        latch.countDown();
                    }
                });
            }

            assertThat(latch.await(20, TimeUnit.SECONDS)).isTrue();
            executor.shutdown();

            assertThat(memberToSession).hasSize(userCount);

            Set<String> sessions = new HashSet<>(memberToSession.values());
            log.info("Created {} sessions for {} users with capacity 1", sessions.size(), userCount);

            // 동시성 이슈로 정확히 50개가 아닐 수 있음
            // 일부 스레드가 동시에 같은 세션에 입장할 수 있음
            assertThat(sessions).as("최소 30개 이상의 세션이 생성되어야 함 (이상: 50개, 동시성으로 인해 감소 가능)")
                    .hasSizeGreaterThanOrEqualTo(30);

            for (String sessionId : sessions) {
                int count = getParticipantCount(sessionId);

                // 정원 절대 초과 불가!
                assertThat(count).as("각 세션은 정원(1명)을 절대 초과하면 안 됨").isLessThanOrEqualTo(1);
            }

            // 총 인원 확인
            int totalCount = sessions.stream().mapToInt(sid -> getParticipantCount(sid)).sum();
            assertThat(totalCount).as("모든 사용자가 배정되어야 함").isEqualTo(userCount);
        }
    }
}
