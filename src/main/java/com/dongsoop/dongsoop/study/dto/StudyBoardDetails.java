package com.dongsoop.dongsoop.study.dto;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StudyBoardDetails {

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

    public StudyBoardDetails(Long id, String title, String content, String tags, LocalDateTime startAt,
                             LocalDateTime endAt, String departmentTypes, String author, LocalDateTime createdAt,
                             LocalDateTime updatedAt, Integer volunteer) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.tags = tags;
        this.startAt = startAt;
        this.endAt = endAt;
        this.departmentTypeList = getDepartmentTypeList(departmentTypes);
        this.author = author;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.volunteer = volunteer;
    }

    private List<DepartmentType> getDepartmentTypeList(String departmentTypes) {
        return Arrays.stream(departmentTypes.split(","))
                .map(String::trim)
                .map(DepartmentType::valueOf)
                .toList();
    }
}
