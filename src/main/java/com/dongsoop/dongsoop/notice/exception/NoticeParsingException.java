package com.dongsoop.dongsoop.notice.exception;

import com.dongsoop.dongsoop.department.entity.Department;

public class NoticeParsingException extends RuntimeException {

    public NoticeParsingException(Department department, Long maxId, Exception e) {
        super("공지사항 파싱 중 오류 발생. department: " + department.getId().name() + ", maxId: " + maxId, e);
    }

}
