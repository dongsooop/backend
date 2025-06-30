package com.dongsoop.dongsoop.mypage.controller;

import com.dongsoop.dongsoop.mypage.dto.MyRecruitmentOverview;
import com.dongsoop.dongsoop.mypage.dto.MyRecruitmentOverviewResponse;
import com.dongsoop.dongsoop.mypage.dto.OpenedMarketplace;
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
    public ResponseEntity<List<MyRecruitmentOverview>> getApplyRecruitmentList(Pageable pageable) {
        List<MyRecruitmentOverview> response = myPageService.getApplyRecruitmentsByMemberId(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/opened-recruitments")
    public ResponseEntity<List<MyRecruitmentOverviewResponse>> getOpenedRecruitmentList(Pageable pageable) {
        List<MyRecruitmentOverviewResponse> response = myPageService.getOpenedRecruitmentsByMemberId(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/opened-marketplace")
    public ResponseEntity<List<OpenedMarketplace>> getOpenedMarketplaceList(Pageable pageable) {
        List<OpenedMarketplace> response = myPageService.getOpenedMarketplacesByMemberId(pageable);
        return ResponseEntity.ok(response);
    }
}
