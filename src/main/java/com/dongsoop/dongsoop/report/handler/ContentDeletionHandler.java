package com.dongsoop.dongsoop.report.handler;

import com.dongsoop.dongsoop.marketplace.repository.MarketplaceBoardRepository;
import com.dongsoop.dongsoop.recruitment.project.repository.ProjectBoardRepository;
import com.dongsoop.dongsoop.recruitment.study.repository.StudyBoardRepository;
import com.dongsoop.dongsoop.recruitment.tutoring.repository.TutoringBoardRepository;
import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ContentDeletionHandler {

    private final Map<ReportType, JpaRepository<?, Long>> contentDeleters;

    public ContentDeletionHandler(ProjectBoardRepository projectBoardRepository,
                                  StudyBoardRepository studyBoardRepository,
                                  MarketplaceBoardRepository marketplaceBoardRepository,
                                  TutoringBoardRepository tutoringBoardRepository) {
        this.contentDeleters = Map.of(
                ReportType.PROJECT_BOARD, projectBoardRepository,
                ReportType.STUDY_BOARD, studyBoardRepository,
                ReportType.MARKETPLACE_BOARD, marketplaceBoardRepository,
                ReportType.TUTORING_BOARD, tutoringBoardRepository
        );
    }

    public void deleteContent(Report report) {
        JpaRepository<?, Long> repository = contentDeleters.get(report.getReportType());

        if (repository != null) {
            repository.deleteById(report.getTargetId());
        }
    }
}