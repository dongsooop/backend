package com.dongsoop.dongsoop.mypage.controller;

import com.dongsoop.dongsoop.mypage.dto.ApplyRecruitment;
import com.dongsoop.dongsoop.mypage.service.MyPageService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;

    @GetMapping("/apply-recruitments")
    public ResponseEntity<List<ApplyRecruitment>> getApplyRecruitmentList(Pageable pageable) {
        List<ApplyRecruitment> response = myPageService.getApplyRecruitmentsByMemberId(pageable);
        return ResponseEntity.ok(response);
    }
}
