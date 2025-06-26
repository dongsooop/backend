package com.dongsoop.dongsoop.report.handler;

import com.dongsoop.dongsoop.marketplace.repository.MarketplaceBoardRepository;
import com.dongsoop.dongsoop.recruitment.project.repository.ProjectBoardRepository;
import com.dongsoop.dongsoop.recruitment.study.repository.StudyBoardRepository;
import com.dongsoop.dongsoop.recruitment.tutoring.repository.TutoringBoardRepository;
import com.dongsoop.dongsoop.report.entity.Report;
import com.dongsoop.dongsoop.report.entity.ReportType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class ContentDeletionHandler {

    private final ProjectBoardRepository projectBoardRepository;
    private final StudyBoardRepository studyBoardRepository;
    private final MarketplaceBoardRepository marketplaceBoardRepository;
    private final TutoringBoardRepository tutoringBoardRepository;

    public void deleteContent(Report report) {
        Optional.ofNullable(getContentDeleters().get(report.getReportType()))
                .ifPresent(deleter -> deleter.accept(report.getTargetId()));
    }

    private Map<ReportType, Consumer<Long>> getContentDeleters() {
        return Map.of(
                ReportType.PROJECT_BOARD, projectBoardRepository::deleteById,
                ReportType.STUDY_BOARD, studyBoardRepository::deleteById,
                ReportType.MARKETPLACE_BOARD, marketplaceBoardRepository::deleteById,
                ReportType.TUTORING_BOARD, tutoringBoardRepository::deleteById
        );
    }
}