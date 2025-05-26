package com.dongsoop.dongsoop.tutoring.dto;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TutoringBoardDetails {

    private Long id;

    private String title;

    private String content;

    private String tags;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    private List<DepartmentType> departmentTypeList;

    private String author;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Integer volunteer;

    public TutoringBoardDetails(Long id, String title, String content, String tags, LocalDateTime startAt,
                                LocalDateTime endAt, DepartmentType departmentType, String author,
                                LocalDateTime createdAt,
                                LocalDateTime updatedAt, Integer volunteer) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.tags = tags;
        this.startAt = startAt;
        this.endAt = endAt;
        this.departmentTypeList = List.of(departmentType);
        this.author = author;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.volunteer = volunteer;
    }
}
