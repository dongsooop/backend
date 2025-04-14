package com.dongsoop.dongsoop.notice.controller;

import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.notice.dto.NoticeListResponse;
import com.dongsoop.dongsoop.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping("/{departmentType}")
    public ResponseEntity<Page<NoticeListResponse>> getNotice(@PathVariable DepartmentType departmentType,
                                                              Pageable pageable) {
        Page<NoticeListResponse> noticeList = noticeService.getNoticeByDepartmentType(departmentType, pageable);
        return ResponseEntity.ok(noticeList);
    }

}
