package com.dongsoop.dongsoop.member.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Column(name = "email", length = 50, unique = true, nullable = false)
    private String email;

    @Column(name = "nickname", length = 20, unique = true, nullable = false)
    private String nickname;

    @Column(name = "password", length = 100, nullable = false)
    private String password;

    @Column(name = "student_id", length = 10)
    private String studentId;

    @Column(name = "department", length = 20)
    private String department;

    @Column(name = "createdAt", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

}
