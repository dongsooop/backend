package com.dongsoop.dongsoop.report.service;

import com.dongsoop.dongsoop.marketplace.repository.MarketplaceBoardRepository;
import com.dongsoop.dongsoop.recruitment.board.project.repository.ProjectBoardRepository;
import com.dongsoop.dongsoop.recruitment.board.study.repository.StudyBoardRepository;
import com.dongsoop.dongsoop.recruitment.board.tutoring.repository.TutoringBoardRepository;
import com.dongsoop.dongsoop.report.entity.ReportType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardContentService {

    private final ProjectBoardRepository projectBoardRepository;
    private final StudyBoardRepository studyBoardRepository;
    private final MarketplaceBoardRepository marketplaceBoardRepository;
    private final TutoringBoardRepository tutoringBoardRepository;

    public String getTitle(Long targetId, ReportType reportType) {
        return getFieldValue(targetId, reportType, "getTitle");
    }

    public String getContent(Long targetId, ReportType reportType) {
        return getFieldValue(targetId, reportType, "getContent");
    }

    private String getFieldValue(Long targetId, ReportType reportType, String methodName) {
        try {
            Object board = getBoard(targetId, reportType);

            if (board == null) {
                return "";
            }

            return invokeMethod(board, methodName);

        } catch (Exception e) {
            log.error("필드 조회 실패 - Method: {}", methodName, e);
            return "";
        }
    }

    private Object getBoard(Long targetId, ReportType reportType) {
        if (ReportType.PROJECT_BOARD.equals(reportType)) {
            return projectBoardRepository.findById(targetId).orElse(null);
        }

        if (ReportType.STUDY_BOARD.equals(reportType)) {
            return studyBoardRepository.findById(targetId).orElse(null);
        }

        if (ReportType.MARKETPLACE_BOARD.equals(reportType)) {
            return marketplaceBoardRepository.findById(targetId).orElse(null);
        }

        if (ReportType.TUTORING_BOARD.equals(reportType)) {
            return tutoringBoardRepository.findById(targetId).orElse(null);
        }

        return null;
    }

    private String invokeMethod(Object board, String methodName) throws Exception {
        Method method = board.getClass().getMethod(methodName);
        Object result = method.invoke(board);

        if (result == null) {
            return "";
        }

        return result.toString();
    }
}