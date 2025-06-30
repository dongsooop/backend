package com.dongsoop.dongsoop.report.handler;

import com.dongsoop.dongsoop.marketplace.repository.MarketplaceBoardRepository;
import com.dongsoop.dongsoop.recruitment.project.repository.ProjectBoardRepository;
import com.dongsoop.dongsoop.recruitment.study.repository.StudyBoardRepository;
import com.dongsoop.dongsoop.recruitment.tutoring.repository.TutoringBoardRepository;
import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ContentDeletionHandler {

    private final ProjectBoardRepository projectBoardRepository;
    private final StudyBoardRepository studyBoardRepository;
    private final MarketplaceBoardRepository marketplaceBoardRepository;
    private final TutoringBoardRepository tutoringBoardRepository;

    public void deleteContent(Report report) {
        JpaRepository<?, Long> repository = getContentDeleters().get(report.getReportType());

        if (repository != null) {
            repository.deleteById(report.getTargetId());
        }
    }

    private Map<ReportType, JpaRepository<?, Long>> getContentDeleters() {
        return Map.of(
                ReportType.PROJECT_BOARD, projectBoardRepository,
                ReportType.STUDY_BOARD, studyBoardRepository,
                ReportType.MARKETPLACE_BOARD, marketplaceBoardRepository,
                ReportType.TUTORING_BOARD, tutoringBoardRepository
        );
    }
}