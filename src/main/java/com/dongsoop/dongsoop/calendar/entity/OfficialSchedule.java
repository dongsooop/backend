package com.dongsoop.dongsoop.calendar.entity;

import com.dongsoop.dongsoop.calendar.dto.ScheduleDetails;
import com.dongsoop.dongsoop.calendar.dto.ScheduleType;
import com.dongsoop.dongsoop.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

@Entity
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SequenceGenerator(name = "official_schedule_sequence_generator")
@SQLRestriction("is_deleted = false")
public class OfficialSchedule extends BaseEntity {

    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "official_schedule_sequence_generator")
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "start_at", nullable = false)
    private LocalDate startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDate endAt;

    public ScheduleDetails toDetails() {
        return ScheduleDetails.builder()
                .id(null)
                .title(title)
                .location("")
                .startAt(startAt.atStartOfDay())
                .endAt(endAt.atTime(23, 59, 59)) // 하루종일
                .type(ScheduleType.OFFICIAL)
                .build();
    }
}
