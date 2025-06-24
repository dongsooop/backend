package com.dongsoop.dongsoop.member.entity;

import com.dongsoop.dongsoop.common.BaseEntity;
import com.dongsoop.dongsoop.department.entity.Department;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "member")
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "email", length = 50, unique = true, nullable = false)
    private String email;

    @Column(name = "nickname", length = 8, unique = true, nullable = false)
    private String nickname;

    @Column(name = "password", length = 100, nullable = false)
    private String password;

    @Column(name = "student_id", length = 10)
    private String studentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    public void delete() {
        this.email = "deleted_" + id; // 이메일을 삭제된 것으로 변경
        this.nickname = "익명_" + id;
        this.password = "deleted";
        this.studentId = null;
        super.isDeleted = true;
        super.updatedAt = LocalDateTime.now();
    }
}
