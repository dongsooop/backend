package com.dongsoop.dongsoop.eclass.dto;

/**
 * 과목-과제 응답을 과제 1건 단위로 평탄화한 값. duedate/cutoffdate/timemodified는 epoch 초이며 duedate/cutoffdate의 0은 "설정 없음"이다.
 */
public record MoodleAssignment(

        long assignId,
        long courseModuleId,
        String courseName,
        String name,
        long dueDate,
        long cutoffDate,
        long timeModified
) {
}
