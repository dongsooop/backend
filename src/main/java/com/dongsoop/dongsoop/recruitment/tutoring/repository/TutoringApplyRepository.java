package com.dongsoop.dongsoop.recruitment.tutoring.repository;

import com.dongsoop.dongsoop.recruitment.tutoring.entity.TutoringApply;
import com.dongsoop.dongsoop.recruitment.tutoring.entity.TutoringApply.TutoringApplyKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TutoringApplyRepository extends JpaRepository<TutoringApply, TutoringApplyKey> {
}
