package com.dongsoop.dongsoop.exception.domain.notice;

import com.dongsoop.dongsoop.department.Department;

public class NoticeParsingException extends RuntimeException {

    public NoticeParsingException(Department department, Long maxId, Exception e) {
        super("공지사항 파싱 중 오류 발생. department: " + department + ", maxId: " + maxId, e);
    }

}
