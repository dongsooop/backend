package com.dongsoop.dongsoop.timetable.entity;

import com.dongsoop.dongsoop.common.BaseEntity;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.timetable.dto.UpdateTimetableRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.Year;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@SuperBuilder
@NoArgsConstructor
@SequenceGenerator(name = "timetable_sequence_generator")
@SQLRestriction("is_deleted = false")
@SQLDelete(sql = "UPDATE timetable SET is_deleted = true WHERE id = ?")
public class Timetable extends BaseEntity {

    @Id
    @GeneratedValue(generator = "timetable_sequence_generator")
    private Long id;

    @Column(name = "name", length = 15, nullable = false)
    private String name;

    @Column(name = "professor", length = 8)
    private String professor;

    @Column(name = "location", length = 10)
    private String location;

    @Column(name = "week", nullable = false)
    private DayOfWeek week;

    @Column(name = "start_at", nullable = false)
    private LocalTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalTime endAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    private Member member;

    @Column(name = "\"year\"", nullable = false, updatable = false)
    private Year year;

    @Column(name = "semester", nullable = false, updatable = false)
    @Enumerated(EnumType.STRING)
    private SemesterType semester;

    public void update(UpdateTimetableRequest request) {
        if (request.name() != null) {
            this.name = request.name();
        }

        if (request.professor() != null) {
            this.professor = request.professor();
        }

        if (request.location() != null) {
            this.location = request.location();
        }

        if (request.week() != null) {
            this.week = request.week();
        }

        if (request.startAt() != null) {
            this.startAt = request.startAt();
        }

        if (request.endAt() != null) {
            this.endAt = request.endAt();
        }
    }
}
