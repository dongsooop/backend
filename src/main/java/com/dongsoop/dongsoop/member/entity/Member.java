package com.dongsoop.dongsoop.member.entity;

import jakarta.persistence.*;
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
public class Member {

    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "email", length = 50, unique = true)
    private String email;

    @Column(name = "nickname", length = 20)
    private String nickname;

    @Column(name = "password", length = 100)
    private String password;

    @Column(name = "student_id", length = 10)
    private String studentId;

    @Column(name = "department", length = 20)
    private String department;

    @Getter
    @Enumerated(EnumType.STRING)
    private Role role;

}
