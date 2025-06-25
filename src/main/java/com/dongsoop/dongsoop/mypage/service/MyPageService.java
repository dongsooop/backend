package com.dongsoop.dongsoop.mypage.service;

import com.dongsoop.dongsoop.mypage.dto.ApplyRecruitment;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface MyPageService {

    List<ApplyRecruitment> getApplyRecruitmentsByMemberId(Pageable pageable);
}
