package com.dongsoop.dongsoop.report.validator;

import com.dongsoop.dongsoop.marketplace.repository.MarketplaceBoardRepository;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.recruitment.board.project.repository.ProjectBoardRepository;
import com.dongsoop.dongsoop.recruitment.board.study.repository.StudyBoardRepository;
import com.dongsoop.dongsoop.recruitment.board.tutoring.repository.TutoringBoardRepository;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.entity.SanctionType;
import com.dongsoop.dongsoop.report.exception.DuplicateReportException;
import com.dongsoop.dongsoop.report.exception.MemberSanctionedException;
import com.dongsoop.dongsoop.report.exception.ReportTargetNotFoundException;
import com.dongsoop.dongsoop.report.exception.SelfReportException;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportValidator {

    private final ReportRepository reportRepository;
    private final ProjectBoardRepository projectBoardRepository;
    private final StudyBoardRepository studyBoardRepository;
    private final MarketplaceBoardRepository marketplaceBoardRepository;
    private final TutoringBoardRepository tutoringBoardRepository;
    private final MemberRepository memberRepository;

    public void checkMemberAccessById(Long memberId) {
        List<SanctionType> banTypes = List.of(SanctionType.TEMPORARY_BAN, SanctionType.PERMANENT_BAN);
        reportRepository.findActiveBanForMember(memberId, LocalDateTime.now(), banTypes)
                .ifPresent(report -> {
                    throw new MemberSanctionedException("회원님은 현재 제재 중입니다. 자세한 내용은 고객센터에 문의해주세요.");
                });
    }

    public void validateAll(Member reporter, ReportType reportType, Long targetId) {
        validateTargetExists(reportType, targetId);
        validateNotSelfReport(reporter, reportType, targetId);
        validateNotDuplicate(reporter, reportType, targetId);
    }

    private void validateTargetExists(ReportType reportType, Long targetId) {
        boolean exists = checkTargetExists(reportType, targetId);

        if (!exists) {
            throw new ReportTargetNotFoundException(reportType.name(), targetId);
        }
    }

    private void validateNotSelfReport(Member reporter, ReportType reportType, Long targetId) {
        boolean isSelfReport = checkSelfReport(reporter, reportType, targetId);

        if (isSelfReport) {
            throw new SelfReportException();
        }
    }

    private void validateNotDuplicate(Member reporter, ReportType reportType, Long targetId) {
        boolean isDuplicate = reportRepository.existsByReporterAndReportTypeAndTargetId(reporter, reportType, targetId);

        if (isDuplicate) {
            throw new DuplicateReportException();
        }
    }

    private boolean checkTargetExists(ReportType reportType, Long targetId) {
        if (ReportType.PROJECT_BOARD.equals(reportType)) {
            return projectBoardRepository.existsById(targetId);
        }

        if (ReportType.STUDY_BOARD.equals(reportType)) {
            return studyBoardRepository.existsById(targetId);
        }

        if (ReportType.MARKETPLACE_BOARD.equals(reportType)) {
            return marketplaceBoardRepository.existsById(targetId);
        }

        if (ReportType.TUTORING_BOARD.equals(reportType)) {
            return tutoringBoardRepository.existsById(targetId);
        }

        if (ReportType.MEMBER.equals(reportType)) {
            return memberRepository.existsById(targetId);
        }

        throw new IllegalArgumentException("Undefined ReportType: " + reportType);
    }

    private boolean checkSelfReport(Member reporter, ReportType reportType, Long targetId) {
        if (ReportType.PROJECT_BOARD.equals(reportType)) {
            return projectBoardRepository.existsByIdAndAuthorId(targetId, reporter.getId());
        }

        if (ReportType.STUDY_BOARD.equals(reportType)) {
            return studyBoardRepository.existsByIdAndAuthorId(targetId, reporter.getId());
        }

        if (ReportType.MARKETPLACE_BOARD.equals(reportType)) {
            return marketplaceBoardRepository.existsByIdAndAuthor(targetId, reporter);
        }

        if (ReportType.TUTORING_BOARD.equals(reportType)) {
            return tutoringBoardRepository.existsByIdAndAuthorId(targetId, reporter.getId());
        }

        if (ReportType.MEMBER.equals(reportType)) {
            return targetId.equals(reporter.getId());
        }

        throw new IllegalArgumentException("Undefined ReportType: " + reportType);
    }
}
