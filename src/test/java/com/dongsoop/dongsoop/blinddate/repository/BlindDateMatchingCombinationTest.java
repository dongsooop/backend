package com.dongsoop.dongsoop.blinddate.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@DisplayName("정상 선택 조합 전수 검증")
class BlindDateMatchingCombinationTest {
    static Stream<Arguments> combinations() {
        List<Arguments> combinations = new ArrayList<>();
        for (int size = 2; size <= 4; size++) {
            int count = (int) Math.pow(size, size);
            for (int code = 0; code < count; code++) {
                Map<Long, Long> choices = new LinkedHashMap<>();
                int remaining = code;
                for (long member = 1; member <= size; member++) {
                    int option = remaining % size;
                    remaining /= size;
                    if (option == 0) continue;
                    long target = option;
                    if (target >= member) target++;
                    choices.put(member, target);
                }
                combinations.add(Arguments.of(size, choices));
            }
        }
        return combinations.stream();
    }

    @ParameterizedTest(name = "M16 {0}명 선택 조합 {1}")
    @MethodSource("combinations")
    void onlyMutualPairsMatchRegardlessOfSubmissionOrder(int size, Map<Long, Long> choices) {
        Set<Long> expectedMatched = new HashSet<>();
        choices.forEach(
                (member, target) -> {
                    if (member.equals(choices.get(target))) {
                        expectedMatched.add(member);
                        expectedMatched.add(target);
                    }
                });
        List<Long> order = new ArrayList<>(choices.keySet());
        verifyOutcome(size, choices, order, expectedMatched);
        java.util.Collections.reverse(order);
        verifyOutcome(size, choices, order, expectedMatched);
    }

    private void verifyOutcome(
            int size, Map<Long, Long> choices, List<Long> order, Set<Long> matched) {
        var storage = new BlindDateParticipantStorageImpl();
        Set<Long> expectedFailed = new HashSet<>();
        for (long member = 1; member <= size; member++) {
            storage.addParticipant("session", member, "socket-" + member);
            if (!matched.contains(member)) expectedFailed.add(member);
        }
        order.forEach(member -> storage.recordChoice("session", member, choices.get(member)));
        for (long member = 1; member <= size; member++) {
            assertThat(storage.isMatched("session", member))
                    .as("회원 %s, 선택 조합 %s", member, choices)
                    .isEqualTo(matched.contains(member));
        }
        assertThat(storage.getNotMatched("session"))
                .containsExactlyInAnyOrderElementsOf(expectedFailed);
    }
}
