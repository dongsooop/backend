package com.dongsoop.dongsoop.blinddate;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.blinddate.support.BlindDateScenarioFixture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(15)
@DisplayName("잘못된 과팅 선택은 매칭으로 처리하지 않는다")
class BlindDateInvalidChoiceScenarioTest {
    @Test
    @DisplayName("V01 자기 자신 선택 거부")
    void selfChoiceCannotCreateMatch() {
        try (var flow = new BlindDateScenarioFixture(2)) {
            String session = flow.joinMembers(2);
            flow.openChoices(session);
            flow.choice.execute(session, 1L, 1L);
            flow.finishChoices();
            assertThat(flow.recipients("/chatroom")).isEmpty();
            assertThat(flow.recipients("/failed")).containsExactlyInAnyOrder(1L, 2L);
        }
    }

    @Test
    @DisplayName("V02 미등록 선택자와 참가자의 상호 선택 거부")
    void unknownChooserCannotMatchParticipant() {
        try (var flow = new BlindDateScenarioFixture(2)) {
            String session = flow.joinMembers(2);
            flow.openChoices(session);
            flow.choice.execute(session, 99L, 1L);
            flow.choice.execute(session, 1L, 99L);
            flow.finishChoices();
            assertThat(flow.recipients("/chatroom")).isEmpty();
            assertThat(flow.recipients("/failed")).containsExactlyInAnyOrder(1L, 2L);
        }
    }

    @Test
    @DisplayName("V03 다른 세션 참가자와의 상호 선택 거부")
    void crossSessionChoiceCannotMatch() {
        try (var flow = new BlindDateScenarioFixture(2)) {
            String first = (String) flow.join(1, "one").get("sessionId");
            flow.join(2, "two");
            flow.join(3, "three");
            flow.join(4, "four");
            flow.openChoices(first);
            flow.choice.execute(first, 1L, 3L);
            flow.choice.execute(first, 3L, 1L);
            flow.finishChoices();
            assertThat(flow.recipients("/chatroom")).isEmpty();
            assertThat(flow.recipients("/failed")).containsExactlyInAnyOrder(1L, 2L);
        }
    }

    @Test
    @DisplayName("V04 null과 존재하지 않는 상대는 성공 이벤트를 만들지 않음")
    void absentTargetsCannotCreateSuccess() {
        try (var flow = new BlindDateScenarioFixture(2)) {
            String session = flow.joinMembers(2);
            flow.openChoices(session);
            flow.choice.execute(session, 1L, null);
            flow.choice.execute(session, 2L, 99L);
            flow.finishChoices();
            assertThat(flow.recipients("/chatroom")).isEmpty();
            assertThat(flow.recipients("/failed")).containsExactlyInAnyOrder(1L, 2L);
        }
    }
}
