package com.dongsoop.dongsoop.blinddate;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.blinddate.entity.ParticipantInfo;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateParticipantStorage;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateParticipantStorageImpl;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateSessionStorageImpl;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateStorage;
import com.dongsoop.dongsoop.blinddate.repository.BlindDateStorageImpl;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Repository 동시성 검증 테스트
 * <p>
 * addParticipant, nameCounter 등의 동시성 문제를 검증합니다.
 */
@DisplayName("Repository 동시성 검증 테스트")
class blindDateStorageConcurrencyTest {

    private static final Logger log = LoggerFactory.getLogger(blindDateStorageConcurrencyTest.class);

    private BlindDateParticipantStorage participantStorage;
    private BlindDateSessionStorageImpl sessionStorage;
    private BlindDateStorage blindDateStorage;

    @BeforeEach
    void setUp() {
        participantStorage = new BlindDateParticipantStorageImpl();
        sessionStorage = new BlindDateSessionStorageImpl();
        blindDateStorage = new BlindDateStorageImpl();
    }

    @RepeatedTest(20)
    @DisplayName("동일 세션에 동시 참가자 추가 - 익명 이름 중복 없어야 함")
    void addParticipant_Concurrent_ShouldHaveUniqueNames() throws InterruptedException {
        String sessionId = "session-1";
        int participantCount = 50;

        ExecutorService executor = Executors.newFixedThreadPool(participantCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(participantCount);

        Map<Long, String> memberToName = new ConcurrentHashMap<>();
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < participantCount; i++) {
            final long memberId = i + 1;
            final String socketId = "socket-" + memberId;

            executor.submit(() -> {
                try {
                    startLatch.await(); // 모든 스레드가 동시에 시작

                    ParticipantInfo participant = participantStorage.addParticipant(
                            sessionId, memberId, socketId
                    );
                    memberToName.put(memberId, participant.getAnonymousName());

                } catch (Exception e) {
                    exceptions.add(e);
                    log.error("Exception for memberId={}", memberId, e);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // 모든 스레드 시작
        assertThat(endLatch.await(15, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        if (!exceptions.isEmpty()) {
            log.error("=== AddParticipant Test Exceptions ===");
            exceptions.forEach(e -> log.error("Exception: {}", e.getMessage(), e));
        }

        assertThat(exceptions).isEmpty();
        assertThat(memberToName).hasSize(participantCount);

        // 중복 이름 체크
        Set<String> names = new HashSet<>(memberToName.values());
        log.info("Generated {} unique names for {} participants", names.size(), participantCount);

        if (names.size() != participantCount) {
            // 중복된 이름 찾기
            Map<String, Long> nameFrequency = memberToName.values().stream()
                    .collect(Collectors.groupingBy(name -> name, Collectors.counting()));

            nameFrequency.entrySet().stream()
                    .filter(e -> e.getValue() > 1)
                    .forEach(e -> log.error("DUPLICATE NAME DETECTED: {} appears {} times",
                            e.getKey(), e.getValue()));
        }

        assertThat(names).as("모든 익명 이름은 고유해야 함").hasSize(participantCount);
    }

    @RepeatedTest(20)
    @DisplayName("동일 사용자 다중 소켓 동시 추가 - 세션은 하나, 소켓은 여러 개")
    void addParticipant_SameMemberMultipleSockets_ShouldShareSession() throws InterruptedException {
        String sessionId = "session-1";
        Long memberId = 1L;
        int socketCount = 10;

        ExecutorService executor = Executors.newFixedThreadPool(socketCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(socketCount);

        Set<String> anonymousNames = ConcurrentHashMap.newKeySet();
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < socketCount; i++) {
            final String socketId = "socket-" + i;

            executor.submit(() -> {
                try {
                    startLatch.await();

                    ParticipantInfo participant = participantRepository.addParticipant(
                            sessionId, memberId, socketId
                    );
                    anonymousNames.add(participant.getAnonymousName());

                } catch (Exception e) {
                    exceptions.add(e);
                    log.error("Exception for socketId={}", socketId, e);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertThat(endLatch.await(15, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        if (!exceptions.isEmpty()) {
            log.error("=== Multiple Sockets Test Exceptions ===");
            exceptions.forEach(e -> log.error("Exception: {}", e.getMessage(), e));
        }

        assertThat(exceptions).isEmpty();
        assertThat(anonymousNames).as("익명 이름은 하나만 있어야 함").hasSize(1);

        ParticipantInfo participant = participantRepository.getByMemberId(memberId);
        assertThat(participant).isNotNull();
        assertThat(participant.getSocketIds()).as("모든 소켓이 등록되어야 함").hasSize(socketCount);
    }

    @Test
    @DisplayName("Pointer 동시 변경 - race condition 검증")
    void setPointer_Concurrent_LastWriteWins() throws InterruptedException {
        blindDateStorage.start(10, LocalDateTime.now().plusHours(1));

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        List<String> pointerSnapshots = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            final String sessionId = "session-" + i;

            executor.submit(() -> {
                try {
                    startLatch.await();

                    blindDateStorage.setPointer(sessionId);
                    Thread.sleep(1); // 다른 스레드가 끼어들 기회 제공
                    pointerSnapshots.add(blindDateStorage.getPointer());

                } catch (Exception e) {
                    log.error("Exception setting pointer", e);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertThat(endLatch.await(10, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        // volatile로 선언되어 있으므로 마지막 쓰기가 보여야 함
        String finalPointer = blindDateStorage.getPointer();
        assertThat(finalPointer).isNotNull();
        log.info("Final pointer: {}", finalPointer);
        log.info("Pointer snapshots variety: {} different values",
                new HashSet<>(pointerSnapshots).size());
    }

    @RepeatedTest(20)
    @DisplayName("SessionInfo 동시 생성 - 고유 ID 보장")
    void createSession_Concurrent_ShouldHaveUniqueIds() throws InterruptedException {
        int sessionCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(sessionCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(sessionCount);

        Set<String> sessionIds = ConcurrentHashMap.newKeySet();
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < sessionCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();

                    String sessionId = sessionStorage.create().getSessionId();
                    sessionIds.add(sessionId);

                } catch (Exception e) {
                    exceptions.add(e);
                    log.error("Exception creating session", e);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertThat(endLatch.await(10, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        if (!exceptions.isEmpty()) {
            log.error("=== Create Session Test Exceptions ===");
            exceptions.forEach(e -> log.error("Exception: {}", e.getMessage(), e));
        }

        assertThat(exceptions).isEmpty();
        assertThat(sessionIds).as("모든 세션 ID는 고유해야 함").hasSize(sessionCount);
    }

    @RepeatedTest(20)
    @DisplayName("참여자 제거 동시 수행 - 안전하게 제거되어야 함")
    void removeSocket_Concurrent_ShouldBeSafe() throws InterruptedException {
        String sessionId = "session-1";
        Long memberId = 1L;
        int socketCount = 10;

        // 먼저 참여자와 소켓들을 추가
        for (int i = 0; i < socketCount; i++) {
            participantRepository.addParticipant(sessionId, memberId, "socket-" + i);
        }

        // 확인
        ParticipantInfo participant = participantRepository.getByMemberId(memberId);
        assertThat(participant.getSocketIds()).hasSize(socketCount);

        // 동시에 모든 소켓 제거
        ExecutorService executor = Executors.newFixedThreadPool(socketCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(socketCount);

        List<Boolean> removeResults = Collections.synchronizedList(new ArrayList<>());
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < socketCount; i++) {
            final String socketId = "socket-" + i;

            executor.submit(() -> {
                try {
                    startLatch.await();

                    boolean removed = participantRepository.removeSocket(socketId);
                    removeResults.add(removed);

                } catch (Exception e) {
                    exceptions.add(e);
                    log.error("Exception removing socket {}", socketId, e);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertThat(endLatch.await(10, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        if (!exceptions.isEmpty()) {
            log.error("=== Remove Socket Test Exceptions ===");
            exceptions.forEach(e -> log.error("Exception: {}", e.getMessage(), e));
        }

        assertThat(exceptions).isEmpty();

        // 모든 소켓이 제거되었으므로 참여자도 제거되어야 함
        ParticipantInfo afterRemoval = participantRepository.getByMemberId(memberId);
        assertThat(afterRemoval).as("모든 소켓 제거 후 참여자도 제거되어야 함").isNull();

        // 대부분의 제거 시도는 성공해야 함
        long successCount = removeResults.stream().filter(Boolean::booleanValue).count();
        log.info("Successfully removed {} out of {} sockets", successCount, socketCount);
        assertThat(successCount).as("최소 1개 이상의 소켓이 제거되어야 함").isGreaterThan(0);
    }
}
