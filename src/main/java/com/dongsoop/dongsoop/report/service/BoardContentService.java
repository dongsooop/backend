package com.dongsoop.dongsoop.report.service;

import com.dongsoop.dongsoop.board.Board;
import com.dongsoop.dongsoop.marketplace.repository.MarketplaceBoardRepository;
import com.dongsoop.dongsoop.recruitment.board.project.repository.ProjectBoardRepository;
import com.dongsoop.dongsoop.recruitment.board.study.repository.StudyBoardRepository;
import com.dongsoop.dongsoop.recruitment.board.tutoring.repository.TutoringBoardRepository;
import com.dongsoop.dongsoop.report.entity.ReportType;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardContentService {

    private final ProjectBoardRepository projectBoardRepository;
    private final StudyBoardRepository studyBoardRepository;
    private final MarketplaceBoardRepository marketplaceBoardRepository;
    private final TutoringBoardRepository tutoringBoardRepository;

    private final Map<ReportType, Function<Long, Optional<? extends Board>>> repositoryMap = Map.of(
            ReportType.PROJECT_BOARD, this::findProjectBoard,
            ReportType.STUDY_BOARD, this::findStudyBoard,
            ReportType.MARKETPLACE_BOARD, this::findMarketplaceBoard,
            ReportType.TUTORING_BOARD, this::findTutoringBoard
    );

    public String getTitle(Long targetId, ReportType reportType) {
        return getBoard(targetId, reportType)
                .map(Board::getTitle)
                .orElse("");
    }

    public String getContent(Long targetId, ReportType reportType) {
        return getBoard(targetId, reportType)
                .map(Board::getContent)
                .orElse("");
    }

    private Optional<Board> getBoard(Long targetId, ReportType reportType) {
        return Optional.ofNullable(repositoryMap.get(reportType))
                .flatMap(repository -> repository.apply(targetId))
                .map(board -> (Board) board);
    }

    private Optional<? extends Board> findProjectBoard(Long id) {
        return projectBoardRepository.findById(id);
    }

    private Optional<? extends Board> findStudyBoard(Long id) {
        return studyBoardRepository.findById(id);
    }

    private Optional<? extends Board> findMarketplaceBoard(Long id) {
        return marketplaceBoardRepository.findById(id);
    }

    private Optional<? extends Board> findTutoringBoard(Long id) {
        return tutoringBoardRepository.findById(id);
    }
}