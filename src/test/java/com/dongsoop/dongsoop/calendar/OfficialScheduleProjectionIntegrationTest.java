package com.dongsoop.dongsoop.calendar;

import static org.assertj.core.api.Assertions.assertThat;

import com.dongsoop.dongsoop.AbstractIntegrationTest;
import com.dongsoop.dongsoop.calendar.dto.HomeSchedule;
import com.dongsoop.dongsoop.calendar.dto.ScheduleType;
import com.dongsoop.dongsoop.calendar.entity.OfficialSchedule;
import com.dongsoop.dongsoop.calendar.repository.OfficialScheduleRepository;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(OfficialScheduleProjectionIntegrationTest.QueryConfig.class)
class OfficialScheduleProjectionIntegrationTest extends AbstractIntegrationTest {
    @Autowired private OfficialScheduleRepository repository;
    @Autowired private EntityManager entityManager;

    @TestConfiguration
    static class QueryConfig {
        @Bean
        JPAQueryFactory queryFactory(EntityManager entityManager) {
            return new JPAQueryFactory(entityManager);
        }
    }

    @Test
    void preservesDateFilterDistinctRangeAndAllDayResponse() {
        LocalDate today = LocalDate.of(2026, 10, 14);
        save("공식 일정", today, today);
        save("공식 일정", today, today);
        save("공식 일정", today, today.plusDays(1));
        save("다음날 일정", today.plusDays(1), today.plusDays(1));
        entityManager.flush();
        entityManager.clear();

        List<HomeSchedule> schedules = repository.searchHomeSchedule(today);

        // 같은 제목이어도 날짜 범위가 다르면 별도 행이며, 같은 범위의 중복만 제거한다.
        assertThat(schedules).hasSize(2).allSatisfy(schedule -> {
            assertThat(schedule.title()).isEqualTo("공식 일정");
            assertThat(schedule.startAt()).isEqualTo(LocalTime.MIN);
            assertThat(schedule.endAt()).isEqualTo(LocalTime.MAX);
            assertThat(schedule.type()).isEqualTo(ScheduleType.OFFICIAL);
        });
        assertThat(repository.searchHomeSchedule(today.minusDays(1))).isEmpty();
    }

    private void save(String title, LocalDate startAt, LocalDate endAt) {
        repository.save(OfficialSchedule.builder().title(title).startAt(startAt).endAt(endAt).build());
    }
}
