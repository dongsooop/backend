package com.dongsoop.dongsoop.notice.exception;

import com.dongsoop.dongsoop.department.entity.Department;

public class NoticeParsingException extends RuntimeException {

    public NoticeParsingException(Department department, int page, Exception e) {
        super("공지사항 파싱 중 오류 발생. department: " + department.getId().name() + ", page: " + page, e);
    }

}
