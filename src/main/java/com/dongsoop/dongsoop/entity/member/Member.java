package com.dongsoop.dongsoop.entity.member;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nickname;

    @Column(name = "student_number", nullable = false)
    private String studentNumber;

    @Column(nullable = false)
    private String department;

    @Builder
    public Member(String email, String password, String nickname, String studentNumber, String department) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.studentNumber = studentNumber;
        this.department = department;
    }
}
