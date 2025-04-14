package com.dongsoop.dongsoop.department.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.Getter;

@Entity
public class Department {

    @Id
    @Enumerated(EnumType.STRING)
    private DepartmentType id;

    private String name;

    @Getter
    private String noticeUrl;
}
