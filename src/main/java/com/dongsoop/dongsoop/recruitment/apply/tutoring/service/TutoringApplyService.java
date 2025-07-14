package com.dongsoop.dongsoop.recruitment.apply.tutoring.service;

import com.dongsoop.dongsoop.recruitment.apply.dto.ApplyDetails;
import com.dongsoop.dongsoop.recruitment.apply.dto.RecruitmentApplyOverview;
import com.dongsoop.dongsoop.recruitment.apply.dto.UpdateApplyStatusRequest;
import com.dongsoop.dongsoop.recruitment.apply.tutoring.dto.ApplyTutoringBoardRequest;
import java.util.List;

public interface TutoringApplyService {

    void apply(ApplyTutoringBoardRequest request);

    void updateStatus(Long boardId, UpdateApplyStatusRequest request);

    List<RecruitmentApplyOverview> getRecruitmentApplyOverview(Long boardId);

    ApplyDetails getRecruitmentApplyDetails(Long boardId, Long applierId);

    ApplyDetails getRecruitmentApplyDetails(Long boardId);
}
