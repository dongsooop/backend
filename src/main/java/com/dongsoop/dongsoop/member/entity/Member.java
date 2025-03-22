package com.dongsoop.dongsoop.member.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;

@Entity
public class Member {

    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;

    @Getter
    private String email;

    @Getter
    private String password;

    @Getter
    @Enumerated(EnumType.STRING)
    private Role role;

    private String nickname;
    private String studentId;
    private String department;

}
