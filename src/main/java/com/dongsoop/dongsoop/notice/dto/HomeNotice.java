package com.dongsoop.dongsoop.notice.dto;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import java.time.LocalDate;

/**
 * 홈 화면에 띄우는 공지 한 건.
 *
 * <p>{@code id} 는 학교 공지 링크에서 뽑은 글 번호라 재크롤링해도 바뀌지 않는다.
 * 앱이 읽은 공지를 로컬에 기록해 두고 이 값으로 대조한다.
 */
public record HomeNotice(

        Long id,
        String title,
        String link,
        LocalDate createdAt,
        NoticeType type
) {
    public HomeNotice(Long id, String title, String link, LocalDate createdAt,
                      DepartmentType departmentType) {
        this(id, title, link, createdAt, getNoticeType(departmentType));
    }

    private static NoticeType getNoticeType(DepartmentType departmentType) {
        if (departmentType.isAllDepartment()) {
            return NoticeType.OFFICIAL;
        }

        return NoticeType.DEPARTMENT;
    }
}
