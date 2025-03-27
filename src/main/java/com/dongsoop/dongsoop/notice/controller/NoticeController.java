package com.dongsoop.dongsoop.notice.controller;

import com.dongsoop.dongsoop.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @PostMapping("/")
    public ResponseEntity<Object> crolled() {
        noticeService.scheduled();
        return ResponseEntity.ok(null);
    }

}
