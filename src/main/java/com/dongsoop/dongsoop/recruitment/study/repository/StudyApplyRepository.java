package com.dongsoop.dongsoop.recruitment.study.repository;

import com.dongsoop.dongsoop.recruitment.study.entity.StudyBoardApply;
import com.dongsoop.dongsoop.recruitment.study.entity.StudyBoardApply.StudyBoardApplyKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyApplyRepository extends JpaRepository<StudyBoardApply, StudyBoardApplyKey> {
}
