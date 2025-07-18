package com.dongsoop.dongsoop.calendar.entity;

import com.dongsoop.dongsoop.calendar.dto.MemberScheduleUpdateRequest;
import com.dongsoop.dongsoop.calendar.dto.ScheduleDetails;
import com.dongsoop.dongsoop.calendar.dto.ScheduleType;
import com.dongsoop.dongsoop.calendar.exception.ScheduleAlreadySetByMemberException;
import com.dongsoop.dongsoop.common.BaseEntity;
import com.dongsoop.dongsoop.member.entity.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@SequenceGenerator(name = "member_schedule_sequence_generator")
@SQLRestriction("is_deleted = false")
@SQLDelete(sql = "UPDATE member_schedule SET is_deleted = true WHERE id = ?")
public class MemberSchedule extends BaseEntity {

    @Id
    @Getter
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "member_schedule_sequence_generator")
    private Long id;

    @Column(name = "title", length = 60, nullable = false)
    private String title;

    @Column(name = "location", length = 20)
    private String location;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    @Getter
    private Member member;

    public void setMember(Member member) {
        if (this.member == null) {
            this.member = member;
            return;
        }

        throw new ScheduleAlreadySetByMemberException(member.getId(), this.member.getId());
    }

    public ScheduleDetails toDetails() {
        return ScheduleDetails.builder()
                .id(id)
                .title(title)
                .location(location)
                .startAt(startAt)
                .endAt(endAt)
                .type(ScheduleType.MEMBER)
                .build();
    }

    public void update(MemberScheduleUpdateRequest request) {
        if (request.getTitle() != null) {
            this.title = request.getTitle();
        }

        if (request.getLocation() != null) {
            this.location = request.getLocation();
        }

        if (request.getStartAt() != null) {
            this.startAt = request.getStartAt();
        }

        if (request.getEndAt() != null) {
            this.endAt = request.getEndAt();
        }
    }
}
