package com.dongsoop.dongsoop.recruitment.apply.project.service;

import com.dongsoop.dongsoop.recruitment.apply.dto.ApplyDetails;
import com.dongsoop.dongsoop.recruitment.apply.dto.RecruitmentApplyOverview;
import com.dongsoop.dongsoop.recruitment.apply.dto.UpdateApplyStatusRequest;
import com.dongsoop.dongsoop.recruitment.apply.project.dto.ApplyProjectBoardRequest;
import java.util.List;

public interface ProjectApplyService {

    void apply(ApplyProjectBoardRequest boardId);

    void updateStatus(Long boardId, UpdateApplyStatusRequest request);

    List<RecruitmentApplyOverview> getRecruitmentApplyOverview(Long boardId);

    ApplyDetails getRecruitmentApplyDetails(Long boardId, Long applierId);

    ApplyDetails getRecruitmentApplyDetails(Long boardId);
}
