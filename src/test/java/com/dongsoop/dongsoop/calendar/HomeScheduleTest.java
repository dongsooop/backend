package com.dongsoop.dongsoop.calendar;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.calendar.dto.HomeSchedule;
import com.dongsoop.dongsoop.calendar.dto.ScheduleType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class HomeScheduleTest {
  @Test
  void allDayPreservesTimesTypeAndJson() {
    HomeSchedule schedule = HomeSchedule.allDay("공식 일정", ScheduleType.OFFICIAL);
    assertThat(schedule.startAt()).isEqualTo(LocalTime.MIN);
    assertThat(schedule.endAt()).isEqualTo(LocalTime.MAX);
    assertThat(schedule.type()).isEqualTo(ScheduleType.OFFICIAL);
    ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    ObjectNode expected = mapper.createObjectNode();
    expected.put("title", "공식 일정");
    expected.set("startAt", mapper.valueToTree(LocalTime.MIN));
    expected.set("endAt", mapper.valueToTree(LocalTime.MAX));
    expected.put("type", "OFFICIAL");
    JsonNode actual = mapper.valueToTree(schedule);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  void memberScheduleKeepsActualTimes() {
    HomeSchedule schedule =
        new HomeSchedule(
            "개인 일정",
            LocalDateTime.of(2026, 10, 14, 9, 30),
            LocalDateTime.of(2026, 10, 14, 11, 0),
            ScheduleType.MEMBER);
    assertThat(schedule.title()).isEqualTo("개인 일정");
    assertThat(schedule.startAt()).isEqualTo(LocalTime.of(9, 30));
    assertThat(schedule.endAt()).isEqualTo(LocalTime.of(11, 0));
    assertThat(schedule.type()).isEqualTo(ScheduleType.MEMBER);
  }
}
