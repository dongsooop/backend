package com.dongsoop.dongsoop.mypage.controller;

import com.dongsoop.dongsoop.mypage.dto.ApplyRecruitment;
import com.dongsoop.dongsoop.mypage.dto.OpenedMarketplace;
import com.dongsoop.dongsoop.mypage.dto.OpenedRecruitment;
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

    @GetMapping("/opened-recruitments")
    public ResponseEntity<List<OpenedRecruitment>> getOpenedRecruitmentList(Pageable pageable) {
        List<OpenedRecruitment> response = myPageService.getOpenedRecruitmentsByMemberId(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/opened-marketplace")
    public ResponseEntity<List<OpenedMarketplace>> getOpenedMarketplaceList(Pageable pageable) {
        List<OpenedMarketplace> response = myPageService.getOpenedMarketplacesByMemberId(pageable);
        return ResponseEntity.ok(response);
    }
}
