package com.dongsoop.dongsoop.recruitment.study.service;

import com.dongsoop.dongsoop.recruitment.dto.ApplyDetails;
import com.dongsoop.dongsoop.recruitment.dto.RecruitmentApplyOverview;
import com.dongsoop.dongsoop.recruitment.dto.UpdateApplyStatusRequest;
import com.dongsoop.dongsoop.recruitment.study.dto.ApplyStudyBoardRequest;
import java.util.List;

public interface StudyApplyService {

    void apply(ApplyStudyBoardRequest request);

    void updateStatus(Long boardId, UpdateApplyStatusRequest request);

    List<RecruitmentApplyOverview> getRecruitmentApplyOverview(Long boardId);

    ApplyDetails getRecruitmentApplyDetails(Long boardId, Long applierId);

    ApplyDetails getRecruitmentApplyDetails(Long boardId);
}
