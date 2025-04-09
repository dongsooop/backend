package com.dongsoop.dongsoop.role.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.Getter;

@Entity
public class Role {

    @Id
    private Long id;

    @Getter
    @Enumerated(EnumType.STRING)
    private RoleType roleType;
}
