package com.dongsoop.dongsoop.department.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Department {

    @Id
    @Getter
    @Enumerated(EnumType.STRING)
    private DepartmentType id;

    private String name;

    @Getter
    private String noticeUrl;

    public boolean equalsId(Department department) {
        return this.id.equals(department.id);
    }
}
