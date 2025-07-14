package com.dongsoop.dongsoop.recruitment.apply.study.service;

import com.dongsoop.dongsoop.recruitment.apply.dto.ApplyDetails;
import com.dongsoop.dongsoop.recruitment.apply.dto.RecruitmentApplyOverview;
import com.dongsoop.dongsoop.recruitment.apply.dto.UpdateApplyStatusRequest;
import com.dongsoop.dongsoop.recruitment.apply.study.dto.ApplyStudyBoardRequest;
import java.util.List;

public interface StudyApplyService {

    void apply(ApplyStudyBoardRequest request);

    void updateStatus(Long boardId, UpdateApplyStatusRequest request);

    List<RecruitmentApplyOverview> getRecruitmentApplyOverview(Long boardId);

    ApplyDetails getRecruitmentApplyDetails(Long boardId, Long applierId);

    ApplyDetails getRecruitmentApplyDetails(Long boardId);
}
