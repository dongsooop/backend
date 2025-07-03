package com.dongsoop.dongsoop.mypage.service;

import com.dongsoop.dongsoop.mypage.dto.MyRecruitmentOverview;
import com.dongsoop.dongsoop.mypage.dto.MyRecruitmentOverviewResponse;
import com.dongsoop.dongsoop.mypage.dto.OpenedMarketplace;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface MyPageService {

    List<MyRecruitmentOverview> getApplyRecruitmentsByMemberId(Pageable pageable);

    List<MyRecruitmentOverviewResponse> getOpenedRecruitmentsByMemberId(Pageable pageable);

    List<OpenedMarketplace> getOpenedMarketplacesByMemberId(Pageable pageable);
}
