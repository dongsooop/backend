package com.dongsoop.dongsoop.mypage.service;

import com.dongsoop.dongsoop.mypage.dto.ApplyRecruitment;
import com.dongsoop.dongsoop.mypage.dto.OpenedMarketplace;
import com.dongsoop.dongsoop.mypage.dto.OpenedRecruitmentResponse;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface MyPageService {

    List<ApplyRecruitment> getApplyRecruitmentsByMemberId(Pageable pageable);

    List<OpenedRecruitmentResponse> getOpenedRecruitmentsByMemberId(Pageable pageable);

    List<OpenedMarketplace> getOpenedMarketplacesByMemberId(Pageable pageable);
}
