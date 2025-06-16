package com.dongsoop.dongsoop.timetable.entity;

import com.dongsoop.dongsoop.common.BaseEntity;
import com.dongsoop.dongsoop.member.entity.Member;
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
import org.hibernate.annotations.SQLRestriction;

@Entity
@SuperBuilder
@NoArgsConstructor
@SequenceGenerator(name = "timetable_sequence_generator")
@SQLRestriction("is_deleted = false")
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
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "year", nullable = false)
    private Year year;

    @Column(name = "semester", nullable = false)
    @Enumerated(EnumType.STRING)
    private SemesterType semester;
}
