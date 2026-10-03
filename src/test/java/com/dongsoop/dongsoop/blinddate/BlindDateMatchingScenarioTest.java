package com.dongsoop.dongsoop.blinddate;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.blinddate.support.BlindDateScenarioFixture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Timeout(15)
@DisplayName("과팅 선택 조합별 최종 결과")
class BlindDateMatchingScenarioTest {
    record Scenario(String name, int members, long[][] choices, Set<Long> matched) {
        @Override
        public String toString() {
            return name;
        }
    }

    static Stream<Scenario> scenarios() {
        return Stream.of(
                new Scenario("M01 아무도 선택하지 않음", 3, new long[][] {}, Set.of()),
                new Scenario("M02 두 명 상호 선택", 2, new long[][] {{1, 2}, {2, 1}}, Set.of(1L, 2L)),
                new Scenario("M03 세 명 중 한 쌍", 3, new long[][] {{1, 2}, {2, 1}}, Set.of(1L, 2L)),
                new Scenario(
                        "M04 전원 두 쌍",
                        4,
                        new long[][] {{1, 2}, {2, 1}, {3, 4}, {4, 3}},
                        Set.of(1L, 2L, 3L, 4L)),
                new Scenario(
                        "M05 두 쌍과 미매칭 한 명",
                        5,
                        new long[][] {{1, 2}, {2, 1}, {3, 4}, {4, 3}, {5, 1}},
                        Set.of(1L, 2L, 3L, 4L)),
                new Scenario("M06 일방 선택", 2, new long[][] {{1, 2}}, Set.of()),
                new Scenario("M07 삼각 순환", 3, new long[][] {{1, 2}, {2, 3}, {3, 1}}, Set.of()),
                new Scenario(
                        "M08 사각 순환", 4, new long[][] {{1, 2}, {2, 3}, {3, 4}, {4, 1}}, Set.of()),
                new Scenario("M09 선택 집중과 무응답", 4, new long[][] {{1, 4}, {2, 4}, {3, 4}}, Set.of()),
                new Scenario(
                        "M10 선택 집중과 한 쌍",
                        4,
                        new long[][] {{1, 4}, {2, 4}, {3, 4}, {4, 2}},
                        Set.of(2L, 4L)),
                new Scenario(
                        "M11 선택이 모두 빗나간 사슬", 4, new long[][] {{1, 2}, {2, 3}, {3, 4}}, Set.of()),
                new Scenario("M12 한 쌍과 미선택 두 명", 4, new long[][] {{1, 2}, {2, 1}}, Set.of(1L, 2L)),
                new Scenario(
                        "M13 중복 재전송",
                        3,
                        new long[][] {{1, 2}, {1, 2}, {2, 1}, {2, 1}},
                        Set.of(1L, 2L)),
                new Scenario(
                        "M14 최초 선택 유지",
                        3,
                        new long[][] {{1, 2}, {1, 3}, {3, 1}, {2, 1}},
                        Set.of(1L, 2L)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void deliversExactlyOneResultPerMember(Scenario scenario) {
        try (var flow = new BlindDateScenarioFixture(scenario.members())) {
            String session = flow.joinMembers(scenario.members());
            flow.openChoices(session);
            for (long[] choice : scenario.choices())
                flow.choice.execute(session, choice[0], choice[1]);
            flow.finishChoices();
            assertResults(flow, session, scenario.members(), scenario.matched());
        }
    }

    @Test
    @DisplayName("M15 동시 상호 선택과 재전송에도 채팅방은 한 개")
    void concurrentMutualChoicesHaveSingleOutcome() throws Exception {
        try (var flow = new BlindDateScenarioFixture(2)) {
            String session = flow.joinMembers(2);
            flow.openChoices(session);
            var executor = Executors.newFixedThreadPool(8);
            var start = new CountDownLatch(1);
            try {
                List<Future<?>> requests = new ArrayList<>();
                for (int i = 0; i < 8; i++) {
                    long member = i % 2 + 1;
                    requests.add(
                            executor.submit(
                                    () -> {
                                        start.await();
                                        flow.choice.execute(session, member, 3 - member);
                                        return null;
                                    }));
                }
                start.countDown();
                for (var request : requests) request.get(5, TimeUnit.SECONDS);
                flow.finishChoices();
                assertResults(flow, session, 2, Set.of(1L, 2L));
            } finally {
                start.countDown();
                executor.shutdownNow();
            }
        }
    }

    private void assertResults(
            BlindDateScenarioFixture flow, String session, int count, Set<Long> matched) {
        Set<Long> failed = new HashSet<>();
        for (long member = 1; member <= count; member++)
            if (!matched.contains(member)) failed.add(member);
        assertThat(flow.recipients("/chatroom")).containsExactlyInAnyOrderElementsOf(matched);
        assertThat(flow.recipients("/failed")).containsExactlyInAnyOrderElementsOf(failed);
        assertThat(flow.results()).hasSize(count);
        assertThat(flow.createdPairs).hasSize(matched.size() / 2);
        for (var pair : flow.createdPairs) {
            assertThat(pair).hasSize(2).doesNotHaveDuplicates();
            assertThat(matched).containsAll(pair);
        }
        for (int index = 0; index < matched.size(); index++) {
            assertThat(flow.results().get(index).destination()).endsWith("/chatroom");
        }
        var successfulPayloads =
                flow.results().stream()
                        .filter(event -> event.destination().endsWith("/chatroom"))
                        .map(event -> (Map<?, ?>) event.payload())
                        .toList();
        for (var payload : successfulPayloads) assertThat(payload.get("chatRoomId")).isNotNull();
        assertThat(flow.sessions.getState(session)).isNull();
    }
}
