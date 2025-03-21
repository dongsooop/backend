package com.dongsoop.dongsoop.member.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Member {

    @Id @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private Long id;
    private String email;
    private String nickname;
    private String password;
    private String studentId;
    private String department;
    @Enumerated(EnumType.STRING)
    private Role role;

}
