package com.dongsoop.dongsoop.notice.entity;

import com.dongsoop.dongsoop.department.DepartmentType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
public class Notice {

    @Id @Getter
    private Long id;

    private String author;

    private String title;

    @Enumerated(EnumType.STRING)
    private DepartmentType type;

    private LocalDate createdAt;

    private String link;
}
