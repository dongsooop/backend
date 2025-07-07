package com.dongsoop.dongsoop.recruitment.tutoring.service;

import com.dongsoop.dongsoop.recruitment.dto.UpdateApplyStatusRequest;
import com.dongsoop.dongsoop.recruitment.tutoring.dto.ApplyTutoringBoardRequest;

public interface TutoringApplyService {

    void apply(ApplyTutoringBoardRequest request);

    void updateStatus(Long boardId, UpdateApplyStatusRequest request);
}
