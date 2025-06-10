package com.dongsoop.dongsoop.recruitment.study.repository;

import com.dongsoop.dongsoop.recruitment.study.entity.StudyApply;
import com.dongsoop.dongsoop.recruitment.study.entity.StudyApply.StudyApplyKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyApplyRepository extends JpaRepository<StudyApply, StudyApplyKey> {
}
