package com.dongsoop.dongsoop.eclass.entity;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "eclass_assignment",
        uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "eclass_id"})
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EclassAssignment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "eclass_id", nullable = false, length = 50)
    private String eclassId;

    @Column(name = "course_id", length = 50)
    private String courseId;

    @Column(name = "course_name", nullable = false, length = 200)
    private String courseName;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "due_date", length = 200)
    private String dueDate;

    @Column(name = "is_submitted", nullable = false)
    private boolean isSubmitted;

    @Column(name = "status", length = 100)
    private String status;

    @Column(name = "link", length = 500)
    private String link;

    public void update(String dueDate, boolean isSubmitted, String status) {
        this.dueDate = dueDate;
        this.isSubmitted = isSubmitted;
        this.status = status;
    }
}