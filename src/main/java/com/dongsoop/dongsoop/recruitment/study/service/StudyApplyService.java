package com.dongsoop.dongsoop.recruitment.study.service;

import com.dongsoop.dongsoop.recruitment.dto.UpdateApplyStatusRequest;
import com.dongsoop.dongsoop.recruitment.study.dto.ApplyStudyBoardRequest;

public interface StudyApplyService {

    void apply(ApplyStudyBoardRequest request);

    void updateStatus(Long boardId, UpdateApplyStatusRequest request);
}
