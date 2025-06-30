package com.dongsoop.dongsoop.report.validator;

import com.dongsoop.dongsoop.exception.domain.report.DuplicateReportException;
import com.dongsoop.dongsoop.exception.domain.report.MemberSanctionedException;
import com.dongsoop.dongsoop.exception.domain.report.ReportTargetNotFoundException;
import com.dongsoop.dongsoop.exception.domain.report.SelfReportException;
import com.dongsoop.dongsoop.marketplace.repository.MarketplaceBoardRepository;
import com.dongsoop.dongsoop.member.entity.Member;
import com.dongsoop.dongsoop.member.repository.MemberRepository;
import com.dongsoop.dongsoop.recruitment.project.repository.ProjectBoardRepository;
import com.dongsoop.dongsoop.recruitment.study.repository.StudyBoardRepository;
import com.dongsoop.dongsoop.recruitment.tutoring.repository.TutoringBoardRepository;
import com.dongsoop.dongsoop.report.entity.ReportType;
import com.dongsoop.dongsoop.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

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
        reportRepository.findActiveBanForMember(memberId, LocalDateTime.now())
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
        Function<Long, Boolean> checker = getExistenceCheckers().get(reportType);
        Boolean exists = checker.apply(targetId);

        if (!exists) {
            throw new ReportTargetNotFoundException(reportType.name(), targetId);
        }
    }

    private void validateNotSelfReport(Member reporter, ReportType reportType, Long targetId) {
        BiFunction<Member, Long, Boolean> checker = getSelfReportCheckers().get(reportType);
        Boolean isSelfReport = checker.apply(reporter, targetId);

        if (isSelfReport) {
            throw new SelfReportException();
        }
    }

    private void validateNotDuplicate(Member reporter, ReportType reportType, Long targetId) {
        Boolean isDuplicate = reportRepository.existsByReporterAndReportTypeAndTargetId(reporter, reportType, targetId);

        if (isDuplicate) {
            throw new DuplicateReportException();
        }
    }

    private Map<ReportType, Function<Long, Boolean>> getExistenceCheckers() {
        return Map.of(
                ReportType.PROJECT_BOARD, projectBoardRepository::existsById,
                ReportType.STUDY_BOARD, studyBoardRepository::existsById,
                ReportType.MARKETPLACE_BOARD, marketplaceBoardRepository::existsById,
                ReportType.TUTORING_BOARD, tutoringBoardRepository::existsById,
                ReportType.MEMBER, memberRepository::existsById
        );
    }

    private Map<ReportType, BiFunction<Member, Long, Boolean>> getSelfReportCheckers() {
        return Map.of(
                ReportType.PROJECT_BOARD, this::checkProjectSelfReport,
                ReportType.STUDY_BOARD, this::checkStudySelfReport,
                ReportType.MARKETPLACE_BOARD, this::checkMarketplaceSelfReport,
                ReportType.TUTORING_BOARD, this::checkTutoringSelfReport,
                ReportType.MEMBER, this::checkMemberSelfReport
        );
    }

    private Boolean checkProjectSelfReport(Member reporter, Long targetId) {
        return projectBoardRepository.existsByIdAndAuthorId(targetId, reporter.getId());
    }

    private Boolean checkStudySelfReport(Member reporter, Long targetId) {
        return studyBoardRepository.existsByIdAndAuthorId(targetId, reporter.getId());
    }

    private Boolean checkMarketplaceSelfReport(Member reporter, Long targetId) {
        return marketplaceBoardRepository.existsByIdAndAuthor(targetId, reporter);
    }

    private Boolean checkTutoringSelfReport(Member reporter, Long targetId) {
        return tutoringBoardRepository.existsByIdAndAuthorId(targetId, reporter.getId());
    }

    private Boolean checkMemberSelfReport(Member reporter, Long targetId) {
        return targetId.equals(reporter.getId());
    }
}