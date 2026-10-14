package com.dongsoop.dongsoop.recruitment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.chat.service.ChatParticipantService;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.recruitment.apply.dto.ApplyDetails;
import com.dongsoop.dongsoop.recruitment.apply.dto.RecruitmentApplyOverview;
import com.dongsoop.dongsoop.recruitment.apply.dto.UpdateApplyStatusRequest;
import com.dongsoop.dongsoop.recruitment.apply.entity.RecruitmentApplyStatus;
import com.dongsoop.dongsoop.recruitment.apply.study.entity.StudyApply;
import com.dongsoop.dongsoop.recruitment.apply.study.exception.StudyApplyNotFoundException;
import com.dongsoop.dongsoop.recruitment.apply.study.notification.StudyApplyNotification;
import com.dongsoop.dongsoop.recruitment.apply.study.repository.StudyApplyRepository;
import com.dongsoop.dongsoop.recruitment.apply.study.repository.StudyApplyRepositoryCustom;
import com.dongsoop.dongsoop.recruitment.apply.study.service.StudyApplyServiceImpl;
import com.dongsoop.dongsoop.recruitment.board.study.entity.StudyBoard;
import com.dongsoop.dongsoop.recruitment.board.study.exception.StudyBoardNotFound;
import com.dongsoop.dongsoop.recruitment.board.study.repository.StudyBoardRepository;
import com.dongsoop.dongsoop.recruitment.board.study.repository.StudyBoardDepartmentRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudyApplyOutcomeServiceTest {
    @Mock MemberService members;
    @Mock StudyApplyRepository applications;
    @Mock StudyApplyRepositoryCustom queries;
    @Mock StudyBoardRepository boards;
    @Mock StudyBoardDepartmentRepository departments;
    @Mock ChatParticipantService chat;
    @Mock StudyApplyNotification notifications;
    @InjectMocks StudyApplyServiceImpl service;

    @Test
    void nonOwnerCannotChangeStatusOrReadApplicants() {
        when(members.getMemberIdByAuthentication()).thenReturn(7L);
        UpdateApplyStatusRequest request = new UpdateApplyStatusRequest(8L, RecruitmentApplyStatus.PASS);
        assertThatThrownBy(() -> service.updateStatus(9L, request)).isInstanceOf(StudyBoardNotFound.class);
        assertThatThrownBy(() -> service.getRecruitmentApplyOverview(9L)).isInstanceOf(StudyBoardNotFound.class);
        assertThatThrownBy(() -> service.getRecruitmentApplyDetails(9L, 8L)).isInstanceOf(StudyBoardNotFound.class);
        verifyNoInteractions(queries, applications, chat, notifications);
    }

    @Test
    void applyStatusDoesNotSendOutcomeOrLoadApplication() {
        owner();
        service.updateStatus(9L, new UpdateApplyStatusRequest(8L, RecruitmentApplyStatus.APPLY));
        verifyNoInteractions(queries, notifications, chat);
    }

    @ParameterizedTest
    @EnumSource(value = RecruitmentApplyStatus.class, names = {"PASS", "FAIL"})
    void missingApplicationCannotSendOutcome(RecruitmentApplyStatus status) {
        owner();
        UpdateApplyStatusRequest request = new UpdateApplyStatusRequest(8L, status);
        assertThatThrownBy(() -> service.updateStatus(9L, request)).isInstanceOf(StudyApplyNotFoundException.class);
        verifyNoInteractions(notifications, chat);
    }

    @Test
    void failedApplicationUpdatesStateAndNotifiesWithoutChatInvite() {
        owner();
        StudyBoard board = StudyBoard.builder().id(9L).title("모집").build();
        StudyApply application = StudyApply.builder().studyBoard(board).build();
        when(queries.findByBoardIdAndApplierId(9L, 8L)).thenReturn(Optional.of(application));
        service.updateStatus(9L, new UpdateApplyStatusRequest(8L, RecruitmentApplyStatus.FAIL));
        assertThat(application).extracting("status").isEqualTo(RecruitmentApplyStatus.FAIL);
        verify(notifications).sendOutcomeNotification(9L, "모집", 8L);
        verifyNoInteractions(chat);
    }

    @Test
    void passedApplicationInvitesIntoLinkedRoom() {
        owner();
        StudyBoard board = StudyBoard.builder().id(9L).title("모집").roomId("room-9").build();
        StudyApply application = StudyApply.builder().studyBoard(board).build();
        when(queries.findByBoardIdAndApplierId(9L, 8L)).thenReturn(Optional.of(application));
        when(boards.findById(9L)).thenReturn(Optional.of(board));
        service.updateStatus(9L, new UpdateApplyStatusRequest(8L, RecruitmentApplyStatus.PASS));
        assertThat(application).extracting("status").isEqualTo(RecruitmentApplyStatus.PASS);
        verify(chat).inviteUserToGroupChat("room-9", 7L, 8L);
        verify(notifications).sendOutcomeNotification(9L, "모집", 8L);
    }

    @Test
    void passedApplicationWithoutRoomStillNotifies() {
        owner();
        StudyBoard board = StudyBoard.builder().id(9L).title("모집").build();
        StudyApply application = StudyApply.builder().studyBoard(board).build();
        when(queries.findByBoardIdAndApplierId(9L, 8L)).thenReturn(Optional.of(application));
        when(boards.findById(9L)).thenReturn(Optional.of(board));
        service.updateStatus(9L, new UpdateApplyStatusRequest(8L, RecruitmentApplyStatus.PASS));
        verifyNoInteractions(chat);
        verify(notifications).sendOutcomeNotification(9L, "모집", 8L);
    }

    @Test
    void ownerGetsRequestedApplicantAndOverview() {
        owner();
        ApplyDetails detail = mock(ApplyDetails.class);
        RecruitmentApplyOverview overview = mock(RecruitmentApplyOverview.class);
        when(queries.findApplyDetailsByBoardIdAndApplierId(9L, 8L)).thenReturn(Optional.of(detail));
        when(applications.findApplyOverviewByBoardId(9L, 7L)).thenReturn(List.of(overview));
        assertThat(service.getRecruitmentApplyDetails(9L, 8L)).isSameAs(detail);
        assertThat(service.getRecruitmentApplyOverview(9L)).containsExactly(overview);
    }

    @Test
    void requesterCanOnlyLoadOwnApplicationThroughOwnEndpoint() {
        when(members.getMemberIdByAuthentication()).thenReturn(7L);
        ApplyDetails detail = mock(ApplyDetails.class);
        when(queries.findApplyDetailsByBoardIdAndApplierId(9L, 7L)).thenReturn(Optional.of(detail));
        assertThat(service.getRecruitmentApplyDetails(9L)).isSameAs(detail);
        verifyNoInteractions(boards);
        assertThatThrownBy(() -> service.getRecruitmentApplyDetails(10L)).isInstanceOf(StudyApplyNotFoundException.class);
    }

    private void owner() {
        when(members.getMemberIdByAuthentication()).thenReturn(7L);
        when(boards.existsByIdAndAuthorId(9L, 7L)).thenReturn(true);
    }
}
