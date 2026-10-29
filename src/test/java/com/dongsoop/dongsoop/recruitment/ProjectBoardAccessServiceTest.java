package com.dongsoop.dongsoop.recruitment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.dongsoop.dongsoop.chat.service.ChatRoomService;
import com.dongsoop.dongsoop.chat.service.ChatService;
import com.dongsoop.dongsoop.chat.util.ChatCommonUtils;
import com.dongsoop.dongsoop.common.exception.authentication.NotAuthenticationException;
import com.dongsoop.dongsoop.department.entity.DepartmentType;
import com.dongsoop.dongsoop.department.repository.DepartmentRepository;
import com.dongsoop.dongsoop.member.service.MemberService;
import com.dongsoop.dongsoop.recruitment.apply.project.repository.ProjectApplyRepositoryCustom;
import com.dongsoop.dongsoop.recruitment.board.dto.RecruitmentDetails;
import com.dongsoop.dongsoop.recruitment.board.dto.RecruitmentOverview;
import com.dongsoop.dongsoop.recruitment.board.project.exception.ProjectBoardNotFound;
import com.dongsoop.dongsoop.recruitment.board.project.repository.ProjectBoardDepartmentRepository;
import com.dongsoop.dongsoop.recruitment.board.project.repository.ProjectBoardRepository;
import com.dongsoop.dongsoop.recruitment.board.project.repository.ProjectBoardRepositoryCustom;
import com.dongsoop.dongsoop.recruitment.board.project.service.ProjectBoardServiceImpl;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ProjectBoardAccessServiceTest {
  @Mock ProjectBoardRepository boards;
  @Mock ProjectBoardRepositoryCustom queries;
  @Mock ProjectApplyRepositoryCustom applies;
  @Mock MemberService members;
  @Mock DepartmentRepository departments;
  @Mock ProjectBoardDepartmentRepository boardDepartments;
  @Mock ChatService chat;
  @Mock ChatRoomService rooms;
  @Mock ChatCommonUtils chatUtils;
  @InjectMocks ProjectBoardServiceImpl service;

  @Test
  void ownerGetsOwnerViewWithoutApplicantLookup() {
    when(members.getMemberIdByAuthentication()).thenReturn(7L);
    when(boards.existsByIdAndAuthorId(9L, 7L)).thenReturn(true);
    RecruitmentDetails details = mock(RecruitmentDetails.class);
    when(queries.findBoardDetailsByIdAndViewType(9L, RecruitmentViewType.OWNER, false))
        .thenReturn(Optional.of(details));
    assertThat(service.getBoardDetailsById(9L)).isSameAs(details);
    verifyNoInteractions(applies);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void memberViewPreservesApplicationState(boolean applied) {
    when(members.getMemberIdByAuthentication()).thenReturn(7L);
    when(applies.existsByBoardIdAndMemberId(9L, 7L)).thenReturn(applied);
    RecruitmentDetails details = mock(RecruitmentDetails.class);
    when(queries.findBoardDetailsByIdAndViewType(9L, RecruitmentViewType.MEMBER, applied))
        .thenReturn(Optional.of(details));
    assertThat(service.getBoardDetailsById(9L)).isSameAs(details);
  }

  @Test
  void guestGetsPublicView() {
    doThrow(new NotAuthenticationException()).when(members).getMemberIdByAuthentication();
    RecruitmentDetails details = mock(RecruitmentDetails.class);
    when(queries.findBoardDetailsByIdAndViewType(9L, RecruitmentViewType.GUEST, false))
        .thenReturn(Optional.of(details));
    assertThat(service.getBoardDetailsById(9L)).isSameAs(details);
    verifyNoInteractions(boards, applies);
  }

  @Test
  void missingBoardHasDomainError() {
    when(members.getMemberIdByAuthentication()).thenReturn(7L);
    assertThatThrownBy(() -> service.getBoardDetailsById(9L))
        .isInstanceOf(ProjectBoardNotFound.class);
  }

  @Test
  void ownerCanDeleteAndOtherMemberCannot() {
    when(members.getMemberIdByAuthentication()).thenReturn(7L);
    when(boards.existsByIdAndAuthorId(9L, 7L)).thenReturn(true);
    service.deleteBoardById(9L);
    verify(boards).deleteById(9L);
    assertThatThrownBy(() -> service.deleteBoardById(10L)).isInstanceOf(ProjectBoardNotFound.class);
    verify(boards, never()).deleteById(10L);
  }

  @Test
  void listPreservesDepartmentAndPage() {
    PageRequest page = PageRequest.of(2, 3);
    RecruitmentOverview overview = mock(RecruitmentOverview.class);
    when(queries.findProjectBoardOverviewsByPage(page)).thenReturn(List.of(overview));
    when(queries.findProjectBoardOverviewsByPageAndDepartmentType(DepartmentType.DEPT_2001, page))
        .thenReturn(List.of(overview));
    assertThat(service.getBoardByPage(page)).containsExactly(overview);
    assertThat(service.getBoardByPageAndDepartmentType(DepartmentType.DEPT_2001, page))
        .containsExactly(overview);
  }
}
