package com.dongsoop.dongsoop.eclass;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.eclass.dto.EclassAssignmentLink;
import com.dongsoop.dongsoop.eclass.notification.EclassReminderMessage;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EclassReminderMessageTest {

    @Test
    @DisplayName("마감이 남았으면 남은 일수를 제목에 넣는다")
    void titleWithRemainingDays() {
        assertThat(EclassReminderMessage.title("자료구조", 3, 20)).isEqualTo("[자료구조] 과제 3일 전입니다");
        assertThat(EclassReminderMessage.title("자료구조", 1, 20)).isEqualTo("[자료구조] 과제 1일 전입니다");
    }

    @Test
    @DisplayName("당일이면 오늘 마감이라고 알린다")
    void titleForToday() {
        assertThat(EclassReminderMessage.title("자료구조", 0, 20)).isEqualTo("[자료구조] 과제 오늘 마감입니다");
    }

    @Test
    @DisplayName("과목명이 길면 잘라서 넣는다")
    void titleWithLongCourseName() {
        String courseName = "가나다라마바사아자차카타파하가나다라마바";

        assertThat(EclassReminderMessage.title(courseName, 3, 20))
                .isEqualTo("[" + courseName + "] 과제 3일 전입니다");
        assertThat(EclassReminderMessage.title(courseName + "사", 3, 20))
                .isEqualTo("[" + courseName + "…] 과제 3일 전입니다");
    }

    @Test
    @DisplayName("본문은 과제명과 마감 시각을 그대로 보여준다")
    void body() {
        assertThat(EclassReminderMessage.body("3주차_과제", LocalDateTime.of(2026, 9, 25, 23, 55)))
                .isEqualTo("3주차_과제 · 마감 9월 25일 (금) 23:55");
    }

    @Test
    @DisplayName("링크는 코스모듈 번호로 만든다")
    void link() {
        assertThat(EclassAssignmentLink.of("https://eclass.dongyang.ac.kr", 9101L))
                .isEqualTo("https://eclass.dongyang.ac.kr/mod/assign/view.php?id=9101");
    }
}
