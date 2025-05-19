package com.dongsoop.dongsoop.tutoring.repository;

import com.dongsoop.dongsoop.tutoring.entity.TutoringApplication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TutoringApplicationRepository extends JpaRepository<TutoringApplication, TutoringApplication.TutoringApplicationKey> {
}
